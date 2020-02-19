package ru.kontur.vostok.hercules.tracing.api.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import ru.kontur.vostok.hercules.tracing.api.Page;
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

    public static String pagedResultAsString(final Page<Event> page) {
        try (
            final StringWriter writer = new StringWriter();
            final JsonGenerator jsonGenerator = FACTORY.createGenerator(writer)
        ) {
            writePagedResult(jsonGenerator, page);
            jsonGenerator.flush();
            writer.flush();
            return writer.toString();
        } catch (IOException e) {
            throw new RuntimeException("Error on creating JSON", e);
        }
    }

    public static void writePagedResult(final JsonGenerator jsonGenerator, final Page<Event> page) throws IOException {
        jsonGenerator.writeStartObject();
        if (Objects.nonNull(page.state())) {
            jsonGenerator.writeStringField("pagingState", page.state());
        }
        jsonGenerator.writeFieldName("result");
        writeEventCollection(jsonGenerator, page.elements());
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
