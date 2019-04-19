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
import ru.kontur.vostok.hercules.protocol.util.EventUtil;
import ru.kontur.vostok.hercules.sink.Sender;
import ru.kontur.vostok.hercules.sink.SenderStatus;
import ru.kontur.vostok.hercules.util.functional.Result;
import ru.kontur.vostok.hercules.util.logging.LoggingConstants;
import ru.kontur.vostok.hercules.util.parsing.Parsers;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;
import ru.kontur.vostok.hercules.util.validation.Validators;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

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

    private final int retryLimit;
    private final boolean retryOnUnknownErrors;
    private final boolean mergePropertiesTagToRoot;

    private final Timer elasticsearchRequestTimeTimer;
    private final Meter elasticsearchRequestErrorsMeter;

    private final Set<String> redefinedExceptions;

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

        this.elasticsearchRequestTimeTimer = metricsCollector.timer("elasticsearchRequestTimeMs");
        this.elasticsearchRequestErrorsMeter = metricsCollector.meter("elasticsearchRequestErrors");
    }

    @Override
    public SenderStatus ping() {
        try {
            Response response = restClient.performRequest("HEAD", "/", Collections.emptyMap());
            return (200 == response.getStatusLine().getStatusCode()) ? SenderStatus.AVAILABLE : SenderStatus.UNAVAILABLE;
        } catch (Exception e) {
            return SenderStatus.UNAVAILABLE;
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

        ByteArrayOutputStream stream = new ByteArrayOutputStream(events.size() * EXPECTED_EVENT_SIZE_BYTES);
        int droppedCount = writeEventRecords(stream, events);
        if (stream.size() == 0) {
            return 0;
        }

        ElasticResponseHandler.Result result;
        try {
            ByteArrayEntity body = new ByteArrayEntity(stream.toByteArray(), ContentType.APPLICATION_JSON);

            int retryCount = retryLimit;
            boolean needToRetry;
            do {
                Response response;
                try (AutoMetricStopwatch requestTime = new AutoMetricStopwatch(elasticsearchRequestTimeTimer, TimeUnit.MILLISECONDS)) {
                    response = restClient.performRequest(
                            "POST",
                            "/_bulk",
                            Collections.emptyMap(),
                            body

                    );
                } catch (IOException ex) {
                    elasticsearchRequestErrorsMeter.mark();
                    throw ex;
                }
                if (response.getStatusLine().getStatusCode() != 200) {
                    elasticsearchRequestErrorsMeter.mark();
                    throw new RuntimeException("Bad response");
                }
                result = elasticResponseHandler.process(response.getEntity(), redefinedExceptions);
                if (result.getTotalErrors() != 0) {
                    LOGGER.info(
                            "Error statistics (retryanble/non retyable/unknown/total): {}/{}/{}/{}",
                            result.getRetryableErrorCount(),
                            result.getNonRetryableErrorCount(),
                            result.getUnknownErrorCount(),
                            result.getTotalErrors()
                    );
                }
                if (result.hasUnknownErrors() && LOGGER.isInfoEnabled()) {
                    for (Event event : events) {
                        if (result.getBadIds().contains(EventUtil.extractStringId(event))) {
                            LOGGER.info("Event caused unknown error: {}", EventFormatter.format(event, false));
                        }
                    }
                }
                needToRetry = result.hasRetryableErrors() || (result.hasUnknownErrors() && retryOnUnknownErrors);
            } while (0 < retryCount-- && needToRetry);
            if (needToRetry) {
                throw new Exception("Have retryable errors in elasticsearch response");
            }
        } catch (Exception e) {
            throw new BackendServiceFailedException(e);
        }

        if (PROCESSED_EVENT_LOGGER.isTraceEnabled()) {
            events.forEach(event -> PROCESSED_EVENT_LOGGER.trace("{},{}", event.getTimestamp(), event.getUuid()));
        }

        droppedCount += result.getTotalErrors();

        return events.size() - droppedCount;

    }

    private int writeEventRecords(OutputStream stream, Collection<Event> events) {
        return toUnchecked(() -> {
            int droppedCount = 0;
            for (Event event : events) {
                boolean result = IndexToElasticJsonWriter.tryWriteIndex(stream, event);
                if (result) {
                    stream.write('\n');
                    EventToElasticJsonWriter.writeEvent(stream, event, mergePropertiesTagToRoot);
                    stream.write('\n');
                } else {
                    droppedCount++;
                    DROPPED_EVENT_LOGGER.trace("{},{}", event.getTimestamp(), event.getUuid());
                }
            }
            return droppedCount;
        });
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
                .withValidator(Validators.greaterThan(0))
                .withDefaultValue(RestClientBuilder.DEFAULT_MAX_CONN_TOTAL)
                .build();

        static final PropertyDescription<Integer> MAX_CONNECTIONS_PER_ROUTE = PropertyDescriptions
                .integerProperty("elastic.maxConnectionsPerRoute")
                .withValidator(Validators.greaterThan(0))
                .withDefaultValue(RestClientBuilder.DEFAULT_MAX_CONN_PER_ROUTE)
                .build();

        static final PropertyDescription<Integer> RETRY_TIMEOUT_MS = PropertyDescriptions
                .integerProperty("elastic.retryTimeoutMs")
                .withValidator(Validators.greaterOrEquals(0))
                .withDefaultValue(RestClientBuilder.DEFAULT_MAX_RETRY_TIMEOUT_MILLIS)
                .build();

        static final PropertyDescription<Integer> CONNECTION_TIMEOUT_MS = PropertyDescriptions
                .integerProperty("elastic.connectionTimeoutMs")
                .withValidator(Validators.greaterOrEquals(0))
                .withDefaultValue(RestClientBuilder.DEFAULT_CONNECT_TIMEOUT_MILLIS)
                .build();

        static final PropertyDescription<Integer> CONNECTION_REQUEST_TIMEOUT_MS = PropertyDescriptions
                .integerProperty("elastic.connectionRequestTimeoutMs")
                .withValidator(Validators.greaterOrEquals(0))
                .withDefaultValue(RestClientBuilder.DEFAULT_CONNECTION_REQUEST_TIMEOUT_MILLIS)
                .build();

        static final PropertyDescription<Integer> SOCKET_TIMEOUT_MS = PropertyDescriptions
                .integerProperty("elastic.socketTimeoutMs")
                .withValidator(Validators.greaterOrEquals(0))
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
    }
}
