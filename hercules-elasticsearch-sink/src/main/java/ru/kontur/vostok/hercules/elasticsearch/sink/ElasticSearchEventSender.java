package ru.kontur.vostok.hercules.elasticsearch.sink;

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
import ru.kontur.vostok.hercules.kafka.util.processing.BulkSender;
import ru.kontur.vostok.hercules.kafka.util.processing.BulkSenderStat;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.util.logging.LoggingConstants;
import ru.kontur.vostok.hercules.util.properties.PropertiesExtractor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static ru.kontur.vostok.hercules.util.throwable.ThrowableUtil.toUnchecked;

public class ElasticSearchEventSender implements BulkSender<Event> {

    private static class ElasticsearchProps {
        static final String HOSTS = "elasticsearch.hosts";

        static final String MAX_CONNECTIONS = "elasticsearch.maxConnections";
        static final String MAX_CONNECTIONS_PER_ROUTE = "elasticsearch.maxConnectionsPerRoute";

        static final String RETRY_TIMEOUT_MS = "elasticsearch.retryTimeoutMs";
        static final String CONNECTION_TIMEOUT_MS = "elasticsearch.connectionTimeoutMs";
        static final String CONNECTION_REQUEST_TIMEOUT_MS = "elasticsearch.connectionRequestTimeoutMs";
        static final String SOCKET_TIMEOUT_MS = "elasticsearch.socketTimeoutMs";
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticSearchEventSender.class);

    private static final Logger RECEIVED_EVENT_LOGGER = LoggerFactory.getLogger(LoggingConstants.RECEIVED_EVENT_LOGGER_NAME);
    private static final Logger PROCESSED_EVENT_LOGGER = LoggerFactory.getLogger(LoggingConstants.PROCESSED_EVENT_LOGGER_NAME);

    private static final int EXPECTED_EVENT_SIZE = 2_048; // in bytes

    private final RestClient restClient;

    private final Timer elasticsearchRequestTimeTimer;
    private final Meter elasticsearchRequestErrorsMeter;

    public ElasticSearchEventSender(
            Properties elasticsearchProperties,
            MetricsCollector metricsCollector
    ) {
        HttpHost[] hosts = parseHosts(PropertiesExtractor.getRequiredProperty(elasticsearchProperties, ElasticsearchProps.HOSTS, String.class));

        int maxConnections = PropertiesExtractor.getAs(elasticsearchProperties, ElasticsearchProps.MAX_CONNECTIONS, Integer.class)
                .orElse(RestClientBuilder.DEFAULT_MAX_CONN_TOTAL);

        int maxConnectionsPerRoute = PropertiesExtractor.getAs(elasticsearchProperties, ElasticsearchProps.MAX_CONNECTIONS_PER_ROUTE, Integer.class)
                .orElse(RestClientBuilder.DEFAULT_MAX_CONN_PER_ROUTE);

        int retryTimeoutMs = PropertiesExtractor.getAs(elasticsearchProperties, ElasticsearchProps.RETRY_TIMEOUT_MS, Integer.class)
                .orElse(RestClientBuilder.DEFAULT_MAX_RETRY_TIMEOUT_MILLIS);

        int connectionTimeout = PropertiesExtractor.getAs(elasticsearchProperties, ElasticsearchProps.CONNECTION_TIMEOUT_MS, Integer.class)
                .orElse(RestClientBuilder.DEFAULT_CONNECT_TIMEOUT_MILLIS);

        int connectionRequestTimeout = PropertiesExtractor.getAs(elasticsearchProperties, ElasticsearchProps.CONNECTION_REQUEST_TIMEOUT_MS, Integer.class)
                .orElse(RestClientBuilder.DEFAULT_CONNECTION_REQUEST_TIMEOUT_MILLIS);

        int socketTimeout = PropertiesExtractor.getAs(elasticsearchProperties, ElasticsearchProps.SOCKET_TIMEOUT_MS, Integer.class)
                .orElse(RestClientBuilder.DEFAULT_SOCKET_TIMEOUT_MILLIS);

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

        this.elasticsearchRequestTimeTimer = metricsCollector.timer("elasticsearchRequestTime");
        this.elasticsearchRequestErrorsMeter = metricsCollector.meter("elasticsearchRequestErrors");
    }

    @Override
    public BulkSenderStat process(Collection<Event> events) {
        if (events.size() == 0) {
            return BulkSenderStat.ZERO;
        }

        events.forEach(event -> RECEIVED_EVENT_LOGGER.trace("{}", event.getId()));

        ByteArrayOutputStream stream = new ByteArrayOutputStream(events.size() * EXPECTED_EVENT_SIZE);
        writeEventRecords(stream, events);

        int errorCount = 0;
        if (0 < stream.size()) {
            long start = System.currentTimeMillis();
            Response response = toUnchecked(() -> restClient.performRequest(
                    "POST",
                    "/_bulk",
                    Collections.emptyMap(),
                    new ByteArrayEntity(stream.toByteArray(), ContentType.APPLICATION_JSON)

            ));
            elasticsearchRequestTimeTimer.update(System.currentTimeMillis() - start, TimeUnit.MILLISECONDS);
            if (response.getStatusLine().getStatusCode() != 200) {
                elasticsearchRequestErrorsMeter.mark();
                throw new RuntimeException("Bad response");
            }
            errorCount = BulkResponseHandler.process(response.getEntity());
        }

        events.forEach(event -> PROCESSED_EVENT_LOGGER.trace("{}", event.getId()));
        return new BulkSenderStat(events.size() - errorCount, errorCount);
    }

    @Override
    public void close() throws Exception {
        restClient.close();
    }


    private void writeEventRecords(OutputStream stream, Collection<Event> events) {
        toUnchecked(() -> {
            for (Event event : events) {
                boolean result = IndexToElasticJsonWriter.tryWriteIndex(stream, event);
                if (result) {
                    writeNewLine(stream);
                    EventToElasticJsonWriter.writeEvent(stream, event);
                    writeNewLine(stream);
                }
                else {
                    LOGGER.error(String.format("Cannot process event '%s' because of missing index data", event.getId()));
                }
            }
        });
    }

    private static void writeNewLine(OutputStream stream) throws IOException {
        stream.write('\n');
    }

    private static HttpHost[] parseHosts(String server) {
        return Arrays.stream(server.split(","))
                .map(HttpHost::create)
                .toArray(HttpHost[]::new);
    }
}
