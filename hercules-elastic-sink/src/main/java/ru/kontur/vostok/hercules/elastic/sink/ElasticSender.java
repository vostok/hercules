package ru.kontur.vostok.hercules.elastic.sink;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import org.apache.http.HttpHost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.health.AutoMetricStopwatch;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.kafka.util.processing.BackendServiceFailedException;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.format.EventFormatter;
import ru.kontur.vostok.hercules.sink.ProcessorStatus;
import ru.kontur.vostok.hercules.sink.Sender;
import ru.kontur.vostok.hercules.util.functional.Result;
import ru.kontur.vostok.hercules.util.logging.LoggingConstants;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.parsing.Parsers;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;
import ru.kontur.vostok.hercules.util.validation.IntegerValidators;
import ru.kontur.vostok.hercules.util.validation.ValidationResult;
import ru.kontur.vostok.hercules.util.validation.Validator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static ru.kontur.vostok.hercules.util.throwable.ThrowableUtil.toUnchecked;

/**
 * @author Gregory Koshelev
 */
public class ElasticSender extends Sender {
    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticSender.class);

    private static final Logger RECEIVED_EVENT_LOGGER = LoggerFactory.getLogger(LoggingConstants.RECEIVED_EVENT_LOGGER_NAME);
    private static final Logger PROCESSED_EVENT_LOGGER = LoggerFactory.getLogger(LoggingConstants.PROCESSED_EVENT_LOGGER_NAME);
    private static final Logger DROPPED_EVENT_LOGGER = LoggerFactory.getLogger(LoggingConstants.DROPPED_EVENT_LOGGER_NAME);

    private static final int EXPECTED_EVENT_SIZE_BYTES = 2_048;

    private final RestClient restClient;
    private final ElasticResponseHandler elasticResponseHandler;
    private final ErrorSender errorSender;

    private final int retryLimit;
    private final boolean retryOnUnknownErrors;
    private final boolean mergePropertiesTagToRoot;

    private final Timer elasticsearchRequestTimeTimer;
    private final Meter elasticsearchRequestErrorsMeter;
    private final Meter elasticsearchDroppedNonRetryableErrorsMeter;

    private final Set<String> redefinedExceptions;
    private final boolean leproseryEnable;
    private final String leproseryIndex;

    private final EventValidator eventValidator;

    public ElasticSender(Properties properties, MetricsCollector metricsCollector) {
        super(properties, metricsCollector);

        this.retryLimit = Props.RETRY_LIMIT.extract(properties);

        HttpHost[] hosts = Props.HOSTS.extract(properties);
        final int maxConnections = Props.MAX_CONNECTIONS.extract(properties);
        final int maxConnectionsPerRoute = Props.MAX_CONNECTIONS_PER_ROUTE.extract(properties);
        final int retryTimeoutMs = Props.RETRY_TIMEOUT_MS.extract(properties);
        final int connectionTimeout = Props.CONNECTION_TIMEOUT_MS.extract(properties);
        final int connectionRequestTimeout = Props.CONNECTION_REQUEST_TIMEOUT_MS.extract(properties);
        final int socketTimeout = Props.SOCKET_TIMEOUT_MS.extract(properties);

        this.leproseryEnable = PropertiesUtil.get(Props.LEPROSERY_MODE, properties).get();
        this.leproseryIndex = PropertiesUtil.get(Props.LEPROSERY_INDEX, properties).get();

        this.redefinedExceptions = new HashSet<>(Arrays.asList(Props.REDEFINED_EXCEPTIONS.extract(properties)));

        this.restClient = RestClient.builder(hosts)
                .setMaxRetryTimeoutMillis(retryTimeoutMs)
                .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                        .setMaxConnTotal(maxConnections)
                        .setMaxConnPerRoute(maxConnectionsPerRoute)
                )
                .setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder
                        .setConnectTimeout(connectionTimeout)
                        .setConnectionRequestTimeout(connectionRequestTimeout)
                        .setSocketTimeout(socketTimeout)
                )
                .build();

        this.retryOnUnknownErrors = Props.RETRY_ON_UNKNOWN_ERRORS.extract(properties);
        this.mergePropertiesTagToRoot = Props.MERGE_PROPERTIES_TAG_TO_ROOT.extract(properties);

        this.elasticResponseHandler = new ElasticResponseHandler(metricsCollector);

        if (leproseryEnable) {
            this.errorSender = new ErrorSender(properties, metricsCollector);
        } else {
            this.errorSender = null;
        }

        this.elasticsearchRequestTimeTimer = metricsCollector.timer("elasticsearchRequestTimeMs");
        this.elasticsearchRequestErrorsMeter = metricsCollector.meter("elasticsearchRequestErrors");
        this.elasticsearchDroppedNonRetryableErrorsMeter = metricsCollector.meter("elasticsearchDroppedNonRetryableErrors");

        this.eventValidator = new EventValidator();
    }

    @Override
    public ProcessorStatus ping() {
        try {
            Response response = restClient.performRequest("HEAD", "/", Collections.emptyMap());
            return (200 == response.getStatusLine().getStatusCode()) ? ProcessorStatus.AVAILABLE : ProcessorStatus.UNAVAILABLE;
        } catch (Exception e) {
            return ProcessorStatus.UNAVAILABLE;
        }
    }

    @Override
    protected int send(List<Event> events) throws BackendServiceFailedException {
        if (events.size() == 0) {
            return 0;
        }

        if (RECEIVED_EVENT_LOGGER.isTraceEnabled()) {
            events.forEach(event -> RECEIVED_EVENT_LOGGER.trace("{},{}", event.getTimestamp(), event.getUuid()));
        }

        int processed = 0;
        Map<String, ValidationResult> badValidationResultMap = new HashMap<>(events.size());
        Map<String, EventWrapper> wrappersMap = events.stream()
                .map(EventWrapper::new)
                .collect(Collectors.toMap(EventWrapper::getId, wrapper -> wrapper));
        try {
            int retryCount = retryLimit;
            boolean needToRetry;
            SendIterationInfo iteration = validateAndPrepare(wrappersMap);
            badValidationResultMap.putAll(iteration.badValidations);
            do {
                ElasticResponseHandler.Result result = trySend(iteration.entity);
                processed += iteration.readyCount - result.getTotalErrors();

                iteration = resultProcess(result, wrappersMap);
                badValidationResultMap.putAll(iteration.badValidations);
                needToRetry = iteration.readyCount > 0;
            } while (0 < retryCount-- && needToRetry);

            if (needToRetry) {
                throw new Exception("Have retryable errors in elasticsearch response");
            }
        } catch (Exception e) {
            throw new BackendServiceFailedException(e);
        }

        if (!badValidationResultMap.isEmpty()) {
            handleNonRetryableErrors(wrappersMap, badValidationResultMap);
        }

        if (PROCESSED_EVENT_LOGGER.isTraceEnabled())
            events.forEach(event -> PROCESSED_EVENT_LOGGER.trace("{},{}", event.getTimestamp(), event.getUuid()));

        return processed;
    }

    private SendIterationInfo validateAndPrepare(Map<String, EventWrapper> wrappers) {
        int ready = 0;
        Map<String, ValidationResult> bads = new HashMap<>(wrappers.size());
        ByteArrayOutputStream stream = new ByteArrayOutputStream(wrappers.size() * EXPECTED_EVENT_SIZE_BYTES);
        for (Map.Entry<String, EventWrapper> entry : wrappers.entrySet()) {
            ValidationResult validationResult = eventValidator.validate(entry.getValue());
            if (validationResult.isOk()) {
                writeEventToStream(stream, entry.getValue());
                ready += 1;
            } else {
                bads.put(entry.getKey(), validationResult);
            }
        }
        ByteArrayEntity entity = new ByteArrayEntity(stream.toByteArray(), ContentType.APPLICATION_JSON);
        return new SendIterationInfo(entity, bads, ready);
    }

    private SendIterationInfo resultProcess(ElasticResponseHandler.Result result, Map<String, EventWrapper> wrappersMap) {
        if (result.getTotalErrors() == 0 ) {
            return new SendIterationInfo(null, Collections.emptyMap(), 0);
        } else {
            LOGGER.info(
                    "Error statistics (retryable/non retryable/unknown/total): {}/{}/{}/{}",
                    result.getRetryableErrorCount(),
                    result.getNonRetryableErrorCount(),
                    result.getUnknownErrorCount(),
                    result.getTotalErrors()
            );

            int ready = 0;
            Map<String, ValidationResult> bads = new HashMap<>(wrappersMap.size());
            ByteArrayOutputStream stream = new ByteArrayOutputStream(wrappersMap.size());
            for (Map.Entry<String, ErrorInfo> errorInfoEntry : result.getErrors().entrySet()) {
                String eventId = errorInfoEntry.getKey();
                EventWrapper wrapper = wrappersMap.get(eventId);
                ErrorInfo errorInfo = errorInfoEntry.getValue();
                ErrorType type = errorInfo.getType();
                if (type.equals(ErrorType.RETRYABLE) || (type.equals(ErrorType.UNKNOWN) && retryOnUnknownErrors)) {
                    writeEventToStream(stream, wrapper);
                    ready += 1;
                } else {
                    bads.put(eventId, ValidationResult.error(errorInfoEntry.getValue().getReason()));
                }
                if (type.equals(ErrorType.UNKNOWN)) {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("Event caused unknown error: {}", EventFormatter.format(wrapper.getEvent(), false));
                    }
                }
            }
            ByteArrayEntity entity = new ByteArrayEntity(stream.toByteArray(), ContentType.APPLICATION_JSON);
            return new SendIterationInfo(entity, bads, ready);
        }
    }

    private void handleNonRetryableErrors(Map<String, EventWrapper> wrappersMap, Map<String, ValidationResult> badValidationResultMap) {
        if (leproseryEnable) {
            errorSender.sendErrors(
                    badValidationResultMap.entrySet().stream()
                            .map(entry -> {
                                String eventId = entry.getKey();
                                EventWrapper eventWrapper = wrappersMap.get(eventId);
                                String index = eventWrapper.getIndex();
                                return EventToLeproseryEvent.toLeproseryEvent(
                                        wrappersMap.get(eventId).getEvent(),
                                        leproseryIndex,
                                        index,
                                        entry.getValue().error()
                                );
                            })
                            .collect(Collectors.toList())
            );
        } else {
            badValidationResultMap.forEach((eventId, validationResult) -> {
                EventWrapper eventWrapper = wrappersMap.get(eventId);
                LOGGER.warn("Non retryable error info: id = {}, index = {}, reason = {}",
                        eventWrapper.getId(),
                        eventWrapper.getIndex(),
                        validationResult.error());
                Event event = eventWrapper.getEvent();
                DROPPED_EVENT_LOGGER.trace("{},{}", event.getTimestamp(), event.getUuid());
            });
            elasticsearchDroppedNonRetryableErrorsMeter.mark(badValidationResultMap.size());
        }
    }

    private void writeEventToStream(ByteArrayOutputStream stream, EventWrapper wrapper) {
        toUnchecked(() -> {
            IndexToElasticJsonWriter.writeIndex(stream, wrapper.getIndex(), wrapper.getId());
            stream.write('\n');
            EventToElasticJsonWriter.writeEvent(stream, wrapper.getEvent(), mergePropertiesTagToRoot);
            stream.write('\n');
        });
    }

    private ElasticResponseHandler.Result trySend(ByteArrayEntity body) {
        Response response;
        try (AutoMetricStopwatch requestTime = new AutoMetricStopwatch(elasticsearchRequestTimeTimer, TimeUnit.MILLISECONDS)) {
            response = restClient.performRequest("POST", "/_bulk", Collections.emptyMap(), body);
        } catch (IOException ex) {
            elasticsearchRequestErrorsMeter.mark();
            throw new RuntimeException(ex);
        }
        if (response.getStatusLine().getStatusCode() != 200) {
            elasticsearchRequestErrorsMeter.mark();
            throw new RuntimeException("Bad response");
        }
        return elasticResponseHandler.process(response.getEntity(), redefinedExceptions);
    }

    private static class Props {
        static final PropertyDescription<Integer> RETRY_LIMIT = PropertyDescriptions
                .integerProperty("retryLimit")
                .withDefaultValue(3)
                .build();

        static final PropertyDescription<Boolean> RETRY_ON_UNKNOWN_ERRORS = PropertyDescriptions
                .booleanProperty("retryOnUnknownErrors")
                .withDefaultValue(Boolean.FALSE)
                .build();

        static final PropertyDescription<HttpHost[]> HOSTS = PropertyDescriptions
                .propertyOfType(HttpHost[].class, "elastic.hosts")
                .withParser(Parsers.parseArray(HttpHost.class, s -> {
                    try {
                        return Result.ok(HttpHost.create(s));
                    } catch (IllegalArgumentException e) {
                        return Result.error(e.getMessage());
                    }
                }))
                .build();

        static final PropertyDescription<Integer> MAX_CONNECTIONS = PropertyDescriptions
                .integerProperty("elastic.maxConnections")
                .withValidator(IntegerValidators.positive())
                .withDefaultValue(RestClientBuilder.DEFAULT_MAX_CONN_TOTAL)
                .build();

        static final PropertyDescription<Integer> MAX_CONNECTIONS_PER_ROUTE = PropertyDescriptions
                .integerProperty("elastic.maxConnectionsPerRoute")
                .withValidator(IntegerValidators.positive())
                .withDefaultValue(RestClientBuilder.DEFAULT_MAX_CONN_PER_ROUTE)
                .build();

        static final PropertyDescription<Integer> RETRY_TIMEOUT_MS = PropertyDescriptions
                .integerProperty("elastic.retryTimeoutMs")
                .withValidator(IntegerValidators.nonNegative())
                .withDefaultValue(RestClientBuilder.DEFAULT_MAX_RETRY_TIMEOUT_MILLIS)
                .build();

        static final PropertyDescription<Integer> CONNECTION_TIMEOUT_MS = PropertyDescriptions
                .integerProperty("elastic.connectionTimeoutMs")
                .withValidator(IntegerValidators.nonNegative())
                .withDefaultValue(RestClientBuilder.DEFAULT_CONNECT_TIMEOUT_MILLIS)
                .build();

        static final PropertyDescription<Integer> CONNECTION_REQUEST_TIMEOUT_MS = PropertyDescriptions
                .integerProperty("elastic.connectionRequestTimeoutMs")
                .withValidator(IntegerValidators.nonNegative())
                .withDefaultValue(RestClientBuilder.DEFAULT_CONNECTION_REQUEST_TIMEOUT_MILLIS)
                .build();

        static final PropertyDescription<Integer> SOCKET_TIMEOUT_MS = PropertyDescriptions
                .integerProperty("elastic.socketTimeoutMs")
                .withValidator(IntegerValidators.nonNegative())
                .withDefaultValue(RestClientBuilder.DEFAULT_SOCKET_TIMEOUT_MILLIS)
                .build();

        static final PropertyDescription<Boolean> MERGE_PROPERTIES_TAG_TO_ROOT = PropertyDescriptions
                .booleanProperty("elastic.mergePropertiesTagToRoot")
                .withDefaultValue(Boolean.FALSE)
                .build();

        static final PropertyDescription<String[]> REDEFINED_EXCEPTIONS = PropertyDescriptions
                .arrayOfStringsProperty("elastic.redefinedExceptions")
                .withDefaultValue(new String[]{})
                .build();

        static final Parameter<Boolean> LEPROSERY_MODE = Parameter
                .booleanParameter("leprosery.enable")
                .withDefault(false)
                .build();

        static final Parameter<String> LEPROSERY_INDEX = Parameter
                .stringParameter("index")
                .withDefault("leprosery")
                .build();
    }

    private static class EventValidator implements Validator<EventWrapper> {

        @Override
        public ValidationResult validate(EventWrapper value) {
            boolean hasIndex = value.getIndex() != null;
            boolean withValidSize = value.getEvent().getBytes().length <= EXPECTED_EVENT_SIZE_BYTES;
            if (hasIndex && withValidSize) {
                return ValidationResult.ok();
            } else if (!hasIndex) {
                return ValidationResult.error("Event has unknown index");
            } else {
                return ValidationResult.error("Event has invalid size");
            }
        }
    }

    private class SendIterationInfo {
        private ByteArrayEntity entity;
        private Map<String, ValidationResult> badValidations;
        private int readyCount;

        SendIterationInfo(ByteArrayEntity entity, Map<String, ValidationResult> badValidations, int readyCount) {
            this.entity = entity;
            this.badValidations = badValidations;
            this.readyCount = readyCount;
        }
    }

}
