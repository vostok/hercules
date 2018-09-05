package ru.kontur.vostok.hercules.elasticsearch.sink;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import org.apache.http.HttpHost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.kafka.util.processing.BulkSender;
import ru.kontur.vostok.hercules.metrics.MetricsCollector;
import ru.kontur.vostok.hercules.protocol.Event;

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

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticSearchEventSender.class);

    private static final int EXPECTED_EVENT_SIZE = 2_048; // in bytes

    private final RestClient restClient;

    private final Timer elasticsearchRequestTimeTimer;
    private final Meter elasticsearchRequestErrorsMeter;

    public ElasticSearchEventSender(Properties elasticsearchProperties, MetricsCollector metricsCollector) {
        HttpHost[] hosts = parseHosts(elasticsearchProperties.getProperty("server"));
        this.restClient = RestClient.builder(hosts).build();

        this.elasticsearchRequestTimeTimer = metricsCollector.timer("elasticsearchRequestTime");
        this.elasticsearchRequestErrorsMeter = metricsCollector.meter("elasticsearchRequestErrors");
    }

    @Override
    public void accept(Collection<Event> events) {
        if (events.size() == 0) {
            return;
        }

        ByteArrayOutputStream stream = new ByteArrayOutputStream(events.size() * EXPECTED_EVENT_SIZE);
        writeEventRecords(stream, events);

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
            BulkResponseHandler.process(response.getEntity());
        }
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
