package ru.kontur.vostok.hercules.elasticsearch.sink;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import ru.kontur.vostok.hercules.protocol.Container;
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
import java.util.Iterator;
import java.util.Map;

public final class EventToElasticJsonWriter {

    @FunctionalInterface
    private interface VariantValueToJsonWriter {
        void write(JsonGenerator generator, Object value) throws IOException;
    }

    private static final String TIMESTAMP_TAG_NAME = "@timestamp";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.nnnnnnnnnX")
            .withZone(ZoneOffset.UTC);

    private static final JsonFactory FACTORY = new JsonFactory();

    private static final VariantValueToJsonWriter[] toJsonWriters = new VariantValueToJsonWriter[256];
    static {
        Arrays.setAll(toJsonWriters, idx -> (g, v) -> {throw new IllegalArgumentException("Not implemented for index " + idx);});

        toJsonWriters[Type.CONTAINER.code] = EventToElasticJsonWriter::writeContainer;
        toJsonWriters[Type.BYTE.code] = EventToElasticJsonWriter::writeByte;
        toJsonWriters[Type.SHORT.code] = EventToElasticJsonWriter::writeShort;
        toJsonWriters[Type.INTEGER.code] = EventToElasticJsonWriter::writeInteger;
        toJsonWriters[Type.LONG.code] = EventToElasticJsonWriter::writeLong;
        toJsonWriters[Type.FLAG.code] = EventToElasticJsonWriter::writeFlag;
        toJsonWriters[Type.FLOAT.code] = EventToElasticJsonWriter::writeFloat;
        toJsonWriters[Type.DOUBLE.code] = EventToElasticJsonWriter::writeDouble;
        toJsonWriters[Type.STRING.code] = EventToElasticJsonWriter::writeStringOrText;
        toJsonWriters[Type.TEXT.code] = EventToElasticJsonWriter::writeStringOrText;

        toJsonWriters[Type.CONTAINER_VECTOR.code] = EventToElasticJsonWriter::writeContainerArrayOrVector;
        toJsonWriters[Type.BYTE_VECTOR.code] = EventToElasticJsonWriter::writeByteArrayOrVector;
        toJsonWriters[Type.SHORT_VECTOR.code] = EventToElasticJsonWriter::writeShortArrayOrVector;
        toJsonWriters[Type.INTEGER_VECTOR.code] = EventToElasticJsonWriter::writeIntegerArrayOrVector;
        toJsonWriters[Type.LONG_VECTOR.code] = EventToElasticJsonWriter::writeLongArrayOrVector;
        toJsonWriters[Type.FLOAT_VECTOR.code] = EventToElasticJsonWriter::writeFloatArrayOrVector;
        toJsonWriters[Type.DOUBLE_VECTOR.code] = EventToElasticJsonWriter::writeDoubleArrayOrVector;
        toJsonWriters[Type.FLAG_VECTOR.code] = EventToElasticJsonWriter::writeFlagArrayOrVector;
        toJsonWriters[Type.STRING_VECTOR.code] = EventToElasticJsonWriter::writeStringOrTextArrayOrVector;
        toJsonWriters[Type.TEXT_VECTOR.code] = EventToElasticJsonWriter::writeStringOrTextArrayOrVector;

        toJsonWriters[Type.CONTAINER_ARRAY.code] = EventToElasticJsonWriter::writeContainerArrayOrVector;
        toJsonWriters[Type.BYTE_ARRAY.code] = EventToElasticJsonWriter::writeByteArrayOrVector;
        toJsonWriters[Type.SHORT_ARRAY.code] = EventToElasticJsonWriter::writeShortArrayOrVector;
        toJsonWriters[Type.INTEGER_ARRAY.code] = EventToElasticJsonWriter::writeIntegerArrayOrVector;
        toJsonWriters[Type.LONG_ARRAY.code] = EventToElasticJsonWriter::writeLongArrayOrVector;
        toJsonWriters[Type.FLOAT_ARRAY.code] = EventToElasticJsonWriter::writeFloatArrayOrVector;
        toJsonWriters[Type.DOUBLE_ARRAY.code] = EventToElasticJsonWriter::writeDoubleArrayOrVector;
        toJsonWriters[Type.FLAG_ARRAY.code] = EventToElasticJsonWriter::writeFlagArrayOrVector;
        toJsonWriters[Type.STRING_ARRAY.code] = EventToElasticJsonWriter::writeStringOrTextArrayOrVector;
        toJsonWriters[Type.TEXT_ARRAY.code] = EventToElasticJsonWriter::writeStringOrTextArrayOrVector;
    }


    public static void writeEvent(OutputStream stream, Event event) throws IOException {
        try (JsonGenerator generator = FACTORY.createGenerator(stream, JsonEncoding.UTF8)) {
            generator.writeStartObject();
            generator.writeStringField(TIMESTAMP_TAG_NAME, FORMATTER.format(TimeUtil.gregorianTicksToInstant(event.getId().timestamp())));

            for (Map.Entry<String, Variant> tag : event.getPayload()) {
                if (TIMESTAMP_TAG_NAME.equals(tag.getKey())) {
                    continue;
                }
                generator.writeFieldName(tag.getKey());
                writeVariantField(generator, tag.getValue());
            }

            generator.writeEndObject();
        }
    }

    private static void writeVariantField(JsonGenerator generator, Variant variant) throws IOException {
        toJsonWriters[variant.getType().code].write(generator, variant.getValue());
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

    private static void writeAsNull(JsonGenerator generator, Object value) throws IOException {
        generator.writeNull();
    }

    private static void writeContainer(JsonGenerator generator, Object value) throws IOException {
        generator.writeStartObject();
        Iterator<Map.Entry<String, Variant>> iterator = ((Container) value).iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Variant> entry = iterator.next();
            generator.writeFieldName(entry.getKey());
            writeVariantField(generator, entry.getValue());
        }
        generator.writeEndObject();
    }

    private static void writeContainerArrayOrVector(JsonGenerator generator, Object value) throws IOException {
        generator.writeStartArray();

        for (Container container : (Container[]) value) {
            writeContainer(generator, container);
        }
        generator.writeEndArray();
    }

    private EventToElasticJsonWriter() {
    }
}
