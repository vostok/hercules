package ru.kontur.hercules.tracing.api.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import ru.kontur.hercules.tracing.api.cassandra.PagedResult;
import ru.kontur.vostok.hercules.json.EventToJsonWriter;
import ru.kontur.vostok.hercules.protocol.Event;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Objects;

/**
 * EventToJsonConverter
 *
 * @author Kirill Sulim
 */
public final class EventToJsonConverter {

    private static final JsonFactory FACTORY = new JsonFactory();

    public static String pagedResultAsString(final PagedResult<Event> pagedResult) {
        try (
            final StringWriter writer = new StringWriter();
            final JsonGenerator jsonGenerator = FACTORY.createGenerator(writer)
        ) {
            writePagedResult(jsonGenerator, pagedResult);
            jsonGenerator.flush();
            writer.flush();
            return writer.toString();
        } catch (IOException e) {
            throw new RuntimeException("Error on creating JSON", e);
        }
    }

    public static void writePagedResult(final JsonGenerator jsonGenerator, final PagedResult<Event> pagedResult) throws IOException {
        jsonGenerator.writeStartObject();
        if (Objects.nonNull(pagedResult.getPagingState())) {
            jsonGenerator.writeStringField("pagingState", pagedResult.getPagingState());
        }
        jsonGenerator.writeFieldName("result");
        writeEventCollection(jsonGenerator, pagedResult.getResult());
        jsonGenerator.writeEndObject();
    }

    public static void writeEventCollection(final JsonGenerator jsonGenerator, final Collection<Event> collection) throws IOException {
        jsonGenerator.writeStartArray();
        for (Event event : collection) {
            writeEventAsJson(jsonGenerator, event);
        }
        jsonGenerator.writeEndArray();
    }

    public static void writeEventAsJson(final JsonGenerator jsonGenerator, final Event event) throws IOException {
        EventToJsonWriter.writeContainer(jsonGenerator, event.getPayload());
    }
}
