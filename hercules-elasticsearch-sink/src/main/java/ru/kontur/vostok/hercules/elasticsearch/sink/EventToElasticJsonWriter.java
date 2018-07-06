package ru.kontur.vostok.hercules.elasticsearch.sink;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;

import static ru.kontur.vostok.hercules.util.throwable.ThrowableUtil.toUnchecked;

public class EventToElasticJsonWriter {

    @FunctionalInterface
    private interface VariantValueToJsonWriter {
        void write(JsonGenerator generator, Object value) throws IOException;
    }

    private static final String TIMESTAMP_FIELD = "@timestamp";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_ZONED_DATE_TIME.withZone(ZoneOffset.UTC);

    private static final JsonFactory FACTORY = new JsonFactory();

    private static final VariantValueToJsonWriter[] toJsonWriters = new VariantValueToJsonWriter[256];
    static {
        Arrays.setAll(toJsonWriters, idx -> (g, v) -> {throw new IllegalArgumentException("Not implemented for index " + idx);});

        toJsonWriters[Type.BYTE.value] = EventToElasticJsonWriter::writeByte;
        toJsonWriters[Type.SHORT.value] = EventToElasticJsonWriter::writeShort;
        toJsonWriters[Type.INTEGER.value] = EventToElasticJsonWriter::writeInteger;
        toJsonWriters[Type.LONG.value] = EventToElasticJsonWriter::writeLong;
        toJsonWriters[Type.FLAG.value] = EventToElasticJsonWriter::writeFlag;
        toJsonWriters[Type.FLOAT.value] = EventToElasticJsonWriter::writeFloat;
        toJsonWriters[Type.DOUBLE.value] = EventToElasticJsonWriter::writeDouble;
        toJsonWriters[Type.STRING.value] = EventToElasticJsonWriter::writeStringOrText;
        toJsonWriters[Type.TEXT.value] = EventToElasticJsonWriter::writeStringOrText;

        toJsonWriters[Type.BYTE_VECTOR.value] = EventToElasticJsonWriter::writeByteArrayOrVector;
        toJsonWriters[Type.SHORT_VECTOR.value] = EventToElasticJsonWriter::writeShortArrayOrVector;
        toJsonWriters[Type.INTEGER_VECTOR.value] = EventToElasticJsonWriter::writeIntegerArrayOrVector;
        toJsonWriters[Type.LONG_VECTOR.value] = EventToElasticJsonWriter::writeLongArrayOrVector;
        toJsonWriters[Type.FLOAT_VECTOR.value] = EventToElasticJsonWriter::writeFloatArrayOrVector;
        toJsonWriters[Type.DOUBLE_VECTOR.value] = EventToElasticJsonWriter::writeDoubleArrayOrVector;
        toJsonWriters[Type.FLAG_VECTOR.value] = EventToElasticJsonWriter::writeFlagArrayOrVector;
        toJsonWriters[Type.STRING_VECTOR.value] = EventToElasticJsonWriter::writeStringOrTextArrayOrVector;
        toJsonWriters[Type.TEXT_VECTOR.value] = EventToElasticJsonWriter::writeStringOrTextArrayOrVector;

        toJsonWriters[Type.BYTE_ARRAY.value] = EventToElasticJsonWriter::writeByteArrayOrVector;
        toJsonWriters[Type.SHORT_ARRAY.value] = EventToElasticJsonWriter::writeShortArrayOrVector;
        toJsonWriters[Type.INTEGER_ARRAY.value] = EventToElasticJsonWriter::writeIntegerArrayOrVector;
        toJsonWriters[Type.LONG_ARRAY.value] = EventToElasticJsonWriter::writeLongArrayOrVector;
        toJsonWriters[Type.FLOAT_ARRAY.value] = EventToElasticJsonWriter::writeFloatArrayOrVector;
        toJsonWriters[Type.DOUBLE_ARRAY.value] = EventToElasticJsonWriter::writeDoubleArrayOrVector;
        toJsonWriters[Type.FLAG_ARRAY.value] = EventToElasticJsonWriter::writeFlagArrayOrVector;
        toJsonWriters[Type.STRING_ARRAY.value] = EventToElasticJsonWriter::writeStringOrTextArrayOrVector;
        toJsonWriters[Type.TEXT_ARRAY.value] = EventToElasticJsonWriter::writeStringOrTextArrayOrVector;
    }


    public static void writeEvent(OutputStream stream, Event event) throws IOException {
        try (JsonGenerator generator = FACTORY.createGenerator(stream, JsonEncoding.UTF8)) {
            generator.writeStartObject();
            generator.writeStringField(TIMESTAMP_FIELD, FORMATTER.format(TimeUtil.gregorianTicksToInstant(event.getId().timestamp())));

            for (Map.Entry<String, Variant> tag : event.getTags().entrySet()) {
                if (TIMESTAMP_FIELD.equals(tag.getKey())) {
                    continue;
                }
                generator.writeFieldName(tag.getKey());
                writeVariantField(generator, tag.getValue());
            }

            generator.writeEndObject();
        }
    }

    private static void writeVariantField(JsonGenerator generator, Variant variant) throws IOException {
        toJsonWriters[variant.getType().value].write(generator, variant.getValue());
    }

    private static void writeByte(JsonGenerator generator, Object value) throws IOException {
        generator.writeNumber((byte) value);
    }

    private static void writeShort(JsonGenerator generator, Object value) throws IOException {
        generator.writeNumber((short) value);
    }

    private static void writeInteger(JsonGenerator generator, Object value) throws IOException {
        generator.writeNumber((int) value);
    }

    private static void writeLong(JsonGenerator generator, Object value) throws IOException {
        generator.writeNumber((long) value);
    }

    private static void writeFloat(JsonGenerator generator, Object value) throws IOException {
        generator.writeNumber((float) value);
    }

    private static void writeDouble(JsonGenerator generator, Object value) throws IOException {
        generator.writeNumber((double) value);
    }

    private static void writeFlag(JsonGenerator generator, Object value) throws IOException {
        generator.writeBoolean((boolean) value);
    }

    private static void writeStringOrText(JsonGenerator generator, Object value) throws IOException {
        generator.writeString(new String((byte[]) value, StandardCharsets.UTF_8));
    }

    private static void writeByteArrayOrVector(JsonGenerator generator, Object value) throws IOException {
        generator.writeStartArray();
        for (byte b : (byte[]) value) {
            generator.writeNumber(b);
        }
        generator.writeEndArray();
    }

    private static void writeShortArrayOrVector(JsonGenerator generator, Object value) throws IOException {
        generator.writeStartArray();
        for (short s : (short[]) value) {
            generator.writeNumber(s);
        }
        generator.writeEndArray();
    }

    private static void writeIntegerArrayOrVector(JsonGenerator generator, Object value) throws IOException {
        generator.writeStartArray();
        for (int i : (int[]) value) {
            generator.writeNumber(i);
        }
        generator.writeEndArray();
    }

    private static void writeLongArrayOrVector(JsonGenerator generator, Object value) throws IOException {
        generator.writeStartArray();
        for (long l : (long[]) value) {
            generator.writeNumber(l);
        }
        generator.writeEndArray();
    }

    private static void writeFloatArrayOrVector(JsonGenerator generator, Object value) throws IOException {
        generator.writeStartArray();
        for (float f : (float[]) value) {
            generator.writeNumber(f);
        }
        generator.writeEndArray();
    }

    private static void writeDoubleArrayOrVector(JsonGenerator generator, Object value) throws IOException {
        generator.writeStartArray();
        for (double d : (double[]) value) {
            generator.writeNumber(d);
        }
        generator.writeEndArray();
    }

    private static void writeFlagArrayOrVector(JsonGenerator generator, Object value) throws IOException {
        generator.writeStartArray();
        for (boolean b : (boolean[]) value) {
            generator.writeBoolean(b);
        }
        generator.writeEndArray();
    }

    private static void writeStringOrTextArrayOrVector(JsonGenerator generator, Object value) throws IOException {
        generator.writeStartArray();
        for (byte[] bytes : (byte[][]) value) {
            generator.writeString(new String(bytes, StandardCharsets.UTF_8));
        }
        generator.writeEndArray();
    }
}
