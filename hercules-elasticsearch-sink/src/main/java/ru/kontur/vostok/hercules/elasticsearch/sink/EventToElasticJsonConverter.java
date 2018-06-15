package ru.kontur.vostok.hercules.elasticsearch.sink;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.util.TimeUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static ru.kontur.vostok.hercules.util.TimeUtil.NANOS_IN_MILLIS;
import static ru.kontur.vostok.hercules.util.throwable.ThrowableUtil.toUnchecked;

public class EventToElasticJsonConverter {

    private static final String TIMESTAMP_FIELD = "@timestamp";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_ZONED_DATE_TIME.withZone(ZoneOffset.UTC);

    private static final JsonFactory FACTORY = new JsonFactory();


    public static void formatEvent(OutputStream stream, Event event) {
        toUnchecked(() -> {
            try (JsonGenerator generator = FACTORY.createGenerator(stream, JsonEncoding.UTF8)) {
                generator.writeStartObject();
                generator.writeStringField(TIMESTAMP_FIELD, FORMATTER.format(fromNanoseconds(event.getTimestamp())));

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
            case FLAG:
                generator.writeBoolean((boolean) variant.getValue());
                break;
            case TEXT:
            case STRING:
                generator.writeString(new String((byte[]) variant.getValue(), StandardCharsets.UTF_8));
                break;
            case BYTE_VECTOR:
            case BYTE_ARRAY:
                generator.writeStartArray();
                for(byte b : (byte[]) variant.getValue()) {
                    generator.writeNumber(b);
                }
                generator.writeEndArray();
                break;
            case SHORT_VECTOR:
            case SHORT_ARRAY:
                generator.writeStartArray();
                for (short s : (short[]) variant.getValue()) {
                    generator.writeNumber(s);
                }
                generator.writeEndArray();
                break;
            case INTEGER_VECTOR:
            case INTEGER_ARRAY:
                generator.writeStartArray();
                for (int i : (int[]) variant.getValue()) {
                    generator.writeNumber(i);
                }
                generator.writeEndArray();
                break;
            case LONG_VECTOR:
            case LONG_ARRAY:
                generator.writeStartArray();
                for (long l : (long[]) variant.getValue()) {
                    generator.writeNumber(l);
                }
                generator.writeEndArray();
                break;
            case FLOAT_VECTOR:
            case FLOAT_ARRAY:
                generator.writeStartArray();
                for (float f : (float[]) variant.getValue()) {
                    generator.writeNumber(f);
                }
                generator.writeEndArray();
                break;
            case DOUBLE_VECTOR:
            case DOUBLE_ARRAY:
                generator.writeStartArray();
                for (double d : (double[]) variant.getValue()) {
                    generator.writeNumber(d);
                }
                generator.writeEndArray();
                break;
            case FLAG_VECTOR:
            case FLAG_ARRAY:
                generator.writeStartArray();
                for (boolean b : (boolean[]) variant.getValue()) {
                    generator.writeBoolean(b);
                }
                generator.writeEndArray();
                break;
            case TEXT_VECTOR:
            case TEXT_ARRAY:
            case STRING_VECTOR:
            case STRING_ARRAY:
                generator.writeStartArray();
                for (byte[] bytes : (byte[][]) variant.getValue()) {
                    generator.writeString(new String(bytes, StandardCharsets.UTF_8));
                }
                generator.writeEndArray();
                break;
            case RESERVED:
            default:
                throw new RuntimeException("Not implemented logic");
        }
    }

    private static Instant fromNanoseconds(long nanoseconds) {
        long millis = nanoseconds / NANOS_IN_MILLIS;
        long nanos = nanoseconds % NANOS_IN_MILLIS;

        return Instant.ofEpochMilli(millis).plusNanos(nanos);
    }
}
