package ru.kontur.vostok.hercules.elasticsearch.sink;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Variant;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static ru.kontur.vostok.hercules.util.throwable.ThrowableUtil.toUnchecked;

public class EventToElasticJsonConverter {

    private static final String TIMESTAMP_FIELD = "@timestamp";

    private static final JsonFactory FACTORY = new JsonFactory();


    public static void formatEvent(OutputStream stream, Event event) {
        toUnchecked(() -> {
            try (JsonGenerator generator = FACTORY.createGenerator(stream, JsonEncoding.UTF8)){
                generator.writeStartObject();
                generator.writeNumberField(TIMESTAMP_FIELD, event.getTimestamp());

                for (Map.Entry<String, Variant> tag : event.getTags().entrySet()) {
                    if (TIMESTAMP_FIELD.equals(tag.getKey())) {
                        continue;
                    }
                    generator.writeFieldName(tag.getKey());
                    writeVariantField(generator, tag.getValue());
                }

                generator.writeEndObject();
            }
        });
    }

    private static void writeVariantField(JsonGenerator generator, Variant variant) throws IOException {
        switch (variant.getType()) {
            case TEXT:
            case STRING:
                generator.writeString(new String((byte[]) variant.getValue(), StandardCharsets.UTF_8));
                break;
            case FLAG:
                generator.writeBoolean((boolean) variant.getValue());
                break;
            case BYTE:
                generator.writeNumber((byte) variant.getValue());
                break;
            case SHORT:
                generator.writeNumber((short) variant.getValue());
                break;
            case INTEGER:
                generator.writeNumber((int) variant.getValue());
                break;
            case LONG:
                generator.writeNumber((long) variant.getValue());
                break;
            case FLOAT:
                generator.writeNumber((float) variant.getValue());
                break;
            case DOUBLE:
                generator.writeNumber((double) variant.getValue());
                break;
            default:
                throw new RuntimeException("Not implemented logic");
        }
    }
}
