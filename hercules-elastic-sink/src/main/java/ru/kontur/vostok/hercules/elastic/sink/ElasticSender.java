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
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.health.AutoMetricStopwatch;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.kafka.util.processing.BackendServiceFailedException;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.sink.ProcessorStatus;
import ru.kontur.vostok.hercules.sink.Sender;
import ru.kontur.vostok.hercules.util.logging.LoggingConstants;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.parameter.parsing.Parsers;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.validation.IntegerValidators;
import ru.kontur.vostok.hercules.util.validation.ValidationResult;

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
import java.util.stream.Stream;

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
    private final LeproserySender leproserySender;

    private final int retryLimit;
    private final boolean retryOnUnknownErrors;
    private final boolean mergePropertiesTagToRoot;

    private final Timer elasticsearchRequestTimeTimer;
    private final Meter elasticsearchRequestErrorsMeter;
    private final Meter elasticsearchDroppedNonRetryableErrorsMeter;

    private final Set<String> redefinedExceptions;
    private final boolean leproseryEnable;

    public ElasticSender(Properties properties, MetricsCollector metricsCollector) {
        super(properties, metricsCollector);

        this.retryLimit = PropertiesUtil.get(Props.RETRY_LIMIT, properties).get();

        HttpHost[] hosts = PropertiesUtil.get(Props.HOSTS, properties).get();
        final int maxConnections = PropertiesUtil.get(Props.MAX_CONNECTIONS, properties).get();
        final int maxConnectionsPerRoute = PropertiesUtil.get(Props.MAX_CONNECTIONS_PER_ROUTE, properties).get();
        final int retryTimeoutMs = PropertiesUtil.get(Props.RETRY_TIMEOUT_MS, properties).get();
        final int connectionTimeout = PropertiesUtil.get(Props.CONNECTION_TIMEOUT_MS, properties).get();
        final int connectionRequestTimeout = PropertiesUtil.get(Props.CONNECTION_REQUEST_TIMEOUT_MS, properties).get();
        final int socketTimeout = PropertiesUtil.get(Props.SOCKET_TIMEOUT_MS, properties).get();

        this.leproseryEnable = PropertiesUtil.get(Props.LEPROSERY_MODE, properties).get();

        this.redefinedExceptions = new HashSet<>(Arrays.asList(PropertiesUtil.get(Props.REDEFINED_EXCEPTIONS, properties).get()));

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

        this.retryOnUnknownErrors = PropertiesUtil.get(Props.RETRY_ON_UNKNOWN_ERRORS, properties).get();
        this.mergePropertiesTagToRoot = PropertiesUtil.get(Props.MERGE_PROPERTIES_TAG_TO_ROOT, properties).get();

        this.elasticResponseHandler = new ElasticResponseHandler(metricsCollector);

        this.leproserySender = leproseryEnable
                ? new LeproserySender(PropertiesUtil.ofScope(properties, Scopes.LEPROSERY), metricsCollector)
                : null;

        this.elasticsearchRequestTimeTimer = metricsCollector.timer("elasticsearchRequestTimeMs");
        this.elasticsearchRequestErrorsMeter = metricsCollector.meter("elasticsearchRequestErrors");
        this.elasticsearchDroppedNonRetryableErrorsMeter = metricsCollector.meter("elasticsearchDroppedNonRetryableErrors");
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
        int droppedCount;
        Map<EventWrapper, ValidationResult> nonRetryableErrorsMap = new HashMap<>(events.size());
        //event-id -> event-wrapper
        Map<String, EventWrapper> readyToSend = new HashMap<>(events.size());
        for (Event event : events) {
            EventWrapper wrapper = new EventWrapper(event);
            ValidationResult validationResult = preValidation(wrapper);
            if (validationResult.isOk()) {
                readyToSend.put(wrapper.getId(), wrapper);
            } else {
                nonRetryableErrorsMap.put(wrapper, validationResult);
            }
        }

        try {
            int retryCount = retryLimit;
            boolean needToRetry;
            do {
                ByteArrayOutputStream dataStream = new ByteArrayOutputStream(readyToSend.size() * EXPECTED_EVENT_SIZE_BYTES);
                readyToSend.values().forEach(wrapper -> writeEventToStream(dataStream, wrapper));
                ElasticResponseHandler.Result result = trySend(new ByteArrayEntity(dataStream.toByteArray(), ContentType.APPLICATION_JSON));

                if (result.getTotalErrors() != 0) {
                    resultProcess(result).forEach((eventId, validationResult) -> {
                        nonRetryableErrorsMap.put(readyToSend.get(eventId), validationResult);
                        readyToSend.remove(eventId);
                    });
                } else {
                    readyToSend.clear();
                }
                needToRetry = readyToSend.size() > 0;
            } while (0 < retryCount-- && needToRetry);

            if (needToRetry) {
                throw new Exception("Have retryable errors in elasticsearch response");
            }

            droppedCount = errorsProcess(nonRetryableErrorsMap);
        } catch (Exception e) {
            throw new BackendServiceFailedException(e);
        }

        if (PROCESSED_EVENT_LOGGER.isTraceEnabled())
            events.forEach(event -> PROCESSED_EVENT_LOGGER.trace("{},{}", event.getTimestamp(), event.getUuid()));

        return events.size() - droppedCount;
    }

    private Map<String, ValidationResult> resultProcess(ElasticResponseHandler.Result result) {
        LOGGER.info(
                "Error statistics (retryable/non retryable/unknown/total): {}/{}/{}/{}",
                result.getRetryableErrorCount(),
                result.getNonRetryableErrorCount(),
                result.getUnknownErrorCount(),
                result.getTotalErrors()
        );

        Map<String, ValidationResult> errorsMap = new HashMap<>(result.getErrors().size());
        for (Map.Entry<String, ErrorInfo> entry : result.getErrors().entrySet()) {
            String eventId = entry.getKey();
            ErrorInfo errorInfo = entry.getValue();
            ErrorType type = errorInfo.getType();
            if (type.equals(ErrorType.NON_RETRYABLE) || (type.equals(ErrorType.UNKNOWN) && !retryOnUnknownErrors)) {
                errorsMap.put(eventId, ValidationResult.error(errorInfo.getReason()));
            }
        }
        return errorsMap;
    }

    /**
     * @return count of dropped events
     */
    private int errorsProcess(Map<EventWrapper, ValidationResult> nonRetryableErrorsInfo) {
        if (nonRetryableErrorsInfo.isEmpty()) {
            return 0;
        }
        if (leproseryEnable) {
            try {
                leproserySender.convertAndSend(nonRetryableErrorsInfo);
                return 0;
            } catch (Exception e) {
                elasticsearchDroppedNonRetryableErrorsMeter.mark(nonRetryableErrorsInfo.size());
                return nonRetryableErrorsInfo.size();
            }
        } else {
            nonRetryableErrorsInfo.forEach((wrapper, validationResult) -> {
                LOGGER.warn("Non retryable error info: id = {}, index = {}, reason = {}",
                        wrapper.getId(),
                        wrapper.getIndex(),
                        validationResult.error());
                Event event = wrapper.getEvent();
                DROPPED_EVENT_LOGGER.trace("{},{}", event.getTimestamp(), event.getUuid());
            });
            elasticsearchDroppedNonRetryableErrorsMeter.mark(nonRetryableErrorsInfo.size());
            return nonRetryableErrorsInfo.size();
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

    private ValidationResult preValidation(EventWrapper wrapper) {
        boolean hasIndex = wrapper.getIndex() != null;
        boolean withValidSize = wrapper.getEvent().getBytes().length <= EXPECTED_EVENT_SIZE_BYTES;
        if (hasIndex && withValidSize) {
            return ValidationResult.ok();
        } else if (!hasIndex) {
            return ValidationResult.error("Event has unknown index");
        } else {
            return ValidationResult.error("Event has invalid size");
        }
    }

    private static class Props {
        static final Parameter<Integer> RETRY_LIMIT = Parameter
                .integerParameter("retryLimit")
                .withDefault(3)
                .withValidator(IntegerValidators.nonNegative())
                .build();

        static final Parameter<Boolean> RETRY_ON_UNKNOWN_ERRORS = Parameter
                .booleanParameter("retryOnUnknownErrors")
                .withDefault(Boolean.FALSE)
                .build();

        static final Parameter<HttpHost[]> HOSTS = Parameter
                .parameter("elastic.hosts",
                        Parsers.fromFunction(str ->
                                Stream.of(str.split(",")).map(HttpHost::create).toArray(HttpHost[]::new)))
                .build();

        static final Parameter<Integer> MAX_CONNECTIONS = Parameter
                .integerParameter("elastic.maxConnections")
                .withValidator(IntegerValidators.positive())
                .withDefault(RestClientBuilder.DEFAULT_MAX_CONN_TOTAL)
                .build();

        static final Parameter<Integer> MAX_CONNECTIONS_PER_ROUTE = Parameter
                .integerParameter("elastic.maxConnectionsPerRoute")
                .withValidator(IntegerValidators.positive())
                .withDefault(RestClientBuilder.DEFAULT_MAX_CONN_PER_ROUTE)
                .build();

        static final Parameter<Integer> RETRY_TIMEOUT_MS = Parameter
                .integerParameter("elastic.retryTimeoutMs")
                .withValidator(IntegerValidators.nonNegative())
                .withDefault(RestClientBuilder.DEFAULT_MAX_RETRY_TIMEOUT_MILLIS)
                .build();

        static final Parameter<Integer> CONNECTION_TIMEOUT_MS = Parameter
                .integerParameter("elastic.connectionTimeoutMs")
                .withValidator(IntegerValidators.nonNegative())
                .withDefault(RestClientBuilder.DEFAULT_CONNECT_TIMEOUT_MILLIS)
                .build();

        static final Parameter<Integer> CONNECTION_REQUEST_TIMEOUT_MS = Parameter
                .integerParameter("elastic.connectionRequestTimeoutMs")
                .withValidator(IntegerValidators.nonNegative())
                .withDefault(RestClientBuilder.DEFAULT_CONNECTION_REQUEST_TIMEOUT_MILLIS)
                .build();

        static final Parameter<Integer> SOCKET_TIMEOUT_MS = Parameter
                .integerParameter("elastic.socketTimeoutMs")
                .withValidator(IntegerValidators.nonNegative())
                .withDefault(RestClientBuilder.DEFAULT_SOCKET_TIMEOUT_MILLIS)
                .build();

        static final Parameter<Boolean> MERGE_PROPERTIES_TAG_TO_ROOT = Parameter
                .booleanParameter("elastic.mergePropertiesTagToRoot")
                .withDefault(Boolean.FALSE)
                .build();

        static final Parameter<String[]> REDEFINED_EXCEPTIONS = Parameter
                .stringArrayParameter("elastic.redefinedExceptions")
                .withDefault(new String[]{})
                .build();

        static final Parameter<Boolean> LEPROSERY_MODE = Parameter
                .booleanParameter("leprosery.enable")
                .withDefault(false)
                .build();
    }

}
