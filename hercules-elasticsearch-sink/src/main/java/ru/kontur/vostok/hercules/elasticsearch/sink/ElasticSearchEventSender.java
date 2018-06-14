package ru.kontur.vostok.hercules.elasticsearch.sink;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.util.throwable.ThrowableUtil;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static ru.kontur.vostok.hercules.util.throwable.ThrowableUtil.wrapException;

public class ElasticSearchEventSender implements AutoCloseable {

    private final RestClient restClient;

    public ElasticSearchEventSender() {
        this.restClient = RestClient.builder(
                new HttpHost("localhost", 9200, "http")
        ).build();
    }

    public void send(Collection<Event> events) {

        Response response = wrapException(() -> restClient.performRequest(
                "POST",
                "/test-elastic-sink/_doc/_bulk",
                Collections.emptyMap(),
                new NStringEntity(buildEventRecords(events), ContentType.APPLICATION_JSON)
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

    private String buildEventRecords(Collection<Event> events) {
        StringBuilder builder = new StringBuilder();
        events.forEach(event -> {
            builder.append("{\"index\":{}}\n");
            formatEvent(builder, event);
        });
        return builder.toString();
    }

    private static void formatEvent(StringBuilder builder, Event event) {
        builder.append("{");
        builder.append("\"@version\":").append(event.getVersion()).append(",");
        builder.append("\"@timestamp\":").append(event.getTimestamp());
        for (Map.Entry<String, Variant> tag : event.getTags().entrySet()) {
            formatTag(builder, tag.getKey(), tag.getValue());
        }
        builder.append("}\n");
    }

    private static void formatTag(StringBuilder builder, String tagName, Variant tagValue) {
        builder.append(",\"").append(tagName).append("\":");
        formatVariantToJsonValue(builder, tagValue);
    }

    private static void formatVariantToJsonValue(StringBuilder builder, Variant variant) {
        switch (variant.getType()) {
            case TEXT:
            case STRING:
                builder.append("\"").append(new String((byte[]) variant.getValue(), StandardCharsets.UTF_8)).append("\"");
                break;
            case FLAG:
                builder.append((boolean) variant.getValue()? "false" : "true");
                break;
            case BYTE:
            case SHORT:
            case INTEGER:
            case LONG:
            case FLOAT:
            case DOUBLE:
                builder.append(variant.getValue());
                break;
            default:
                throw new RuntimeException("Not implemented logic");
        }
    }
}
