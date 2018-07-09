package ru.kontur.vostok.hercules.elasticsearch.sink;

import org.apache.http.HttpHost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import ru.kontur.vostok.hercules.kafka.util.processing.BulkEventSender;
import ru.kontur.vostok.hercules.kafka.util.processing.Entry;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.util.TagExtractor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static ru.kontur.vostok.hercules.util.throwable.ThrowableUtil.toUnchecked;

public class ElasticSearchEventSender implements BulkEventSender {

    private static final int EXPECTED_EVENT_SIZE = 2_048; // in bytes

    private final RestClient restClient;
    private final String indexName;

    public ElasticSearchEventSender(Properties elasticsearchProperties) {
        HttpHost[] hosts = parseHosts(elasticsearchProperties.getProperty("server"));
        this.restClient = RestClient.builder(hosts).build();
        this.indexName = elasticsearchProperties.getProperty("index.name");
    }

    public void send(Collection<Entry<UUID, Event>> events) {
        if (events.size() == 0) {
            return;
        }

        ByteArrayOutputStream stream = new ByteArrayOutputStream(events.size() * EXPECTED_EVENT_SIZE);
        writeEventRecords(stream, events);

        if (0 < stream.size()) {
            Response response = toUnchecked(() -> restClient.performRequest(
                    "POST",
                    "/" + indexName + "/_doc/_bulk",
                    Collections.emptyMap(),
                    new ByteArrayEntity(stream.toByteArray(), ContentType.APPLICATION_JSON)

            ));
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("Bad response");
            }
            BulkResponseHandler.process(response.getEntity());
        }
    }

    @Override
    public void close() throws Exception {
        restClient.close();
    }


    private void writeEventRecords(OutputStream stream, Collection<Entry<UUID, Event>> events) {
        toUnchecked(() -> {
            for (Entry<UUID, Event> entry : events) {
                boolean result = IndexToElasticJsonWriter.writeIndex(stream, entry.getValue());
                if (result) {
                    writeNewLine(stream);
                    EventToElasticJsonWriter.writeEvent(stream, entry.getValue());
                    writeNewLine(stream);
                }
                else {
                    // TODO: Add logging
                    System.out.println(String.format("Cannot process event '%s' because of missing index data", entry.getValue().getId()));
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
