package ru.kontur.vostok.hercules.elasticsearch.sink;

import org.apache.http.HttpHost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import ru.kontur.vostok.hercules.protocol.Event;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static ru.kontur.vostok.hercules.util.throwable.ThrowableUtil.toUnchecked;

public class ElasticSearchEventSender implements AutoCloseable {

    private static final int EXPECTED_EVENT_SIZE = 2_048; // in bytes

    private static final byte[] EMPTY_INDEX = "{\"index\":{}}".getBytes(StandardCharsets.UTF_8);

    private final RestClient restClient;

    public ElasticSearchEventSender() {
        this.restClient = RestClient.builder(
                new HttpHost("localhost", 9200, "http")
        ).build();
    }

    public void send(Collection<BulkProcessor.Entry<Void, Event>> events) {
        if (events.size() == 0) {
            return;
        }

        ByteArrayOutputStream stream = new ByteArrayOutputStream(events.size() * EXPECTED_EVENT_SIZE);
        writeEventRecords(stream, events);

        Response response = toUnchecked(() -> restClient.performRequest(
                "POST",
                "/test-elastic-sink/_doc/_bulk",
                Collections.emptyMap(),
                new ByteArrayEntity(stream.toByteArray(), ContentType.APPLICATION_JSON)

        ));
        // FIXME тут все непросто, ошибки каждой записи надо обрабатывать отдельно
        if (response.getStatusLine().getStatusCode() != 200) {
            throw new RuntimeException("Bad response");
        }
    }

    @Override
    public void close() throws Exception {
        restClient.close();
    }

    private void writeEventRecords(OutputStream stream, Collection<BulkProcessor.Entry<Void, Event>> events) {
        events.forEach(entry -> {
            writeEmptyIndex(stream);
            writeNewLine(stream);
            EventToElasticJsonConverter.formatEvent(stream, entry.getValue());
            writeNewLine(stream);
        });
    }

    private static void writeEmptyIndex(OutputStream stream) {
        toUnchecked(() -> stream.write(EMPTY_INDEX));
    }

    private static void writeNewLine(OutputStream stream) {
        toUnchecked(() -> stream.write('\n'));
    }
}
