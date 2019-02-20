package ru.kontur.vostok.hercules.elastic.sink;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.Vector;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.tags.CommonTags;
import ru.kontur.vostok.hercules.tags.ElasticSearchTags;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class EventToElasticJsonWriter {

    @FunctionalInterface
    private interface VariantValueToJsonWriter {
        void write(JsonGenerator generator, Object value) throws IOException;
    }

    private static final String TIMESTAMP_TAG_NAME = "@timestamp";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.nnnnnnnnnX")
            .withZone(ZoneOffset.UTC);

    private static final Set<String> IGNORED_TAGS = new HashSet<>(Arrays.asList(
            TIMESTAMP_TAG_NAME,
            ElasticSearchTags.INDEX_PATTERN_TAG.getName()
    ));

    private static final JsonFactory FACTORY = new JsonFactory();

    private static final VariantValueToJsonWriter[] TO_JSON_WRITERS = new VariantValueToJsonWriter[256];

    private static final VariantValueToJsonWriter[] VECTOR_TO_JSON_WRITERS = new VariantValueToJsonWriter[256];

    static {
        Arrays.setAll(TO_JSON_WRITERS, idx -> (g, v) -> {
            throw new IllegalArgumentException("Not implemented for index " + idx);
        });

        TO_JSON_WRITERS[Type.CONTAINER.code] = EventToElasticJsonWriter::writeContainer;
        TO_JSON_WRITERS[Type.BYTE.code] = EventToElasticJsonWriter::writeByte;
        TO_JSON_WRITERS[Type.SHORT.code] = EventToElasticJsonWriter::writeShort;
        TO_JSON_WRITERS[Type.INTEGER.code] = EventToElasticJsonWriter::writeInteger;
        TO_JSON_WRITERS[Type.LONG.code] = EventToElasticJsonWriter::writeLong;
        TO_JSON_WRITERS[Type.FLAG.code] = EventToElasticJsonWriter::writeFlag;
        TO_JSON_WRITERS[Type.FLOAT.code] = EventToElasticJsonWriter::writeFloat;
        TO_JSON_WRITERS[Type.DOUBLE.code] = EventToElasticJsonWriter::writeDouble;
        TO_JSON_WRITERS[Type.STRING.code] = EventToElasticJsonWriter::writeString;
        TO_JSON_WRITERS[Type.UUID.code] = EventToElasticJsonWriter::writeUuid;
        TO_JSON_WRITERS[Type.NULL.code] = EventToElasticJsonWriter::writeAsNull;
        TO_JSON_WRITERS[Type.VECTOR.code] = EventToElasticJsonWriter::writeVector;
    }

    static {
        Arrays.setAll(VECTOR_TO_JSON_WRITERS, idx -> (g, v) -> {
            throw new IllegalArgumentException("Not implemented for index " + idx);
        });

        VECTOR_TO_JSON_WRITERS[Type.CONTAINER.code] = EventToElasticJsonWriter::writeContainerVector;
        VECTOR_TO_JSON_WRITERS[Type.BYTE.code] = EventToElasticJsonWriter::writeByteVector;
        VECTOR_TO_JSON_WRITERS[Type.SHORT.code] = EventToElasticJsonWriter::writeShortVector;
        VECTOR_TO_JSON_WRITERS[Type.INTEGER.code] = EventToElasticJsonWriter::writeIntegerVector;
        VECTOR_TO_JSON_WRITERS[Type.LONG.code] = EventToElasticJsonWriter::writeLongVector;
        VECTOR_TO_JSON_WRITERS[Type.FLOAT.code] = EventToElasticJsonWriter::writeFloatVector;
        VECTOR_TO_JSON_WRITERS[Type.DOUBLE.code] = EventToElasticJsonWriter::writeDoubleVector;
        VECTOR_TO_JSON_WRITERS[Type.FLAG.code] = EventToElasticJsonWriter::writeFlagVector;
        VECTOR_TO_JSON_WRITERS[Type.STRING.code] = EventToElasticJsonWriter::writeStringVector;
        VECTOR_TO_JSON_WRITERS[Type.UUID.code] = EventToElasticJsonWriter::writeUuidVector;
        VECTOR_TO_JSON_WRITERS[Type.NULL.code] = EventToElasticJsonWriter::writeNullVector;
        VECTOR_TO_JSON_WRITERS[Type.VECTOR.code] = EventToElasticJsonWriter::writeVectorVector;
    }


    public static void writeEvent(OutputStream stream, Event event, boolean mergePropertiesToRoot) throws IOException {
        try (JsonGenerator generator = FACTORY.createGenerator(stream, JsonEncoding.UTF8)) {
            generator.writeStartObject();
            generator.writeStringField(TIMESTAMP_TAG_NAME, FORMATTER.format(TimeUtil.unixTicksToInstant(event.getTimestamp())));

            if (mergePropertiesToRoot) {
                final Optional<Container> properties = ContainerUtil.extract(event.getPayload(), CommonTags.PROPERTIES_TAG);
                if (properties.isPresent()) {
                    for (Map.Entry<String, Variant> tag : properties.get()) {
                        writeVariantAsField(generator, tag.getKey(), tag.getValue());
                    }
                }
            }

            for (Map.Entry<String, Variant> tag : event.getPayload()) {
                if (IGNORED_TAGS.contains(tag.getKey())
                    || (mergePropertiesToRoot && CommonTags.PROPERTIES_TAG.getName().equals(tag.getKey()))
                ) {
                    continue;
                }
                writeVariantAsField(generator, tag.getKey(), tag.getValue());
            }

            generator.writeEndObject();
        }
    }

    private static void writeVariantAsField(JsonGenerator generator, String tagName, Variant variant) throws IOException {
        generator.writeFieldName(tagName);
        writeVariantValue(generator, variant);
    }

    private static void writeVariantValue(JsonGenerator generator, Variant variant) throws IOException {
        TO_JSON_WRITERS[variant.getType().code].write(generator, variant.getValue());
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

    private static void writeString(JsonGenerator generator, Object value) throws IOException {
        generator.writeString(new String((byte[]) value, StandardCharsets.UTF_8));
    }

    private static void writeUuid(JsonGenerator generator, Object value) throws IOException {
        generator.writeString(((UUID)value).toString());
    }

    private static void writeVector(JsonGenerator generator, Object value) throws IOException {
        Vector vector = (Vector) value;
        VECTOR_TO_JSON_WRITERS[vector.getType().code].write(generator, vector.getValue());
    }

    private static void writeByteVector(JsonGenerator generator, Object value) throws IOException {
        generator.writeStartArray();
        for (byte b : (byte[]) value) {
            generator.writeNumber(b);
        }
        generator.writeEndArray();
    }

    private static void writeShortVector(JsonGenerator generator, Object value) throws IOException {
        generator.writeStartArray();
        for (short s : (short[]) value) {
            generator.writeNumber(s);
        }
        generator.writeEndArray();
    }

    private static void writeIntegerVector(JsonGenerator generator, Object value) throws IOException {
        generator.writeStartArray();
        for (int i : (int[]) value) {
            generator.writeNumber(i);
        }
        generator.writeEndArray();
    }

    private static void writeLongVector(JsonGenerator generator, Object value) throws IOException {
        generator.writeStartArray();
        for (long l : (long[]) value) {
            generator.writeNumber(l);
        }
        generator.writeEndArray();
    }

    private static void writeFloatVector(JsonGenerator generator, Object value) throws IOException {
        generator.writeStartArray();
        for (float f : (float[]) value) {
            generator.writeNumber(f);
        }
        generator.writeEndArray();
    }

    private static void writeDoubleVector(JsonGenerator generator, Object value) throws IOException {
        generator.writeStartArray();
        for (double d : (double[]) value) {
            generator.writeNumber(d);
        }
        generator.writeEndArray();
    }

    private static void writeFlagVector(JsonGenerator generator, Object value) throws IOException {
        generator.writeStartArray();
        for (boolean b : (boolean[]) value) {
            generator.writeBoolean(b);
        }
        generator.writeEndArray();
    }

    private static void writeStringVector(JsonGenerator generator, Object value) throws IOException {
        generator.writeStartArray();
        for (byte[] bytes : (byte[][]) value) {
            generator.writeString(new String(bytes, StandardCharsets.UTF_8));
        }
        generator.writeEndArray();
    }

    private static void writeUuidVector(JsonGenerator generator, Object value) throws IOException {
        generator.writeStartArray();
        for (UUID uuid : (UUID[]) value) {
            generator.writeString(uuid.toString());
        }
        generator.writeEndArray();
    }

    private static void writeNullVector(JsonGenerator generator, Object value) throws IOException {
        generator.writeStartArray();
        Object[] nulls = (Object[]) value;
        for (int i = 0; i < nulls.length; i++) {
            generator.writeNull();
        }
        generator.writeEndArray();
    }

    private static void writeVectorVector(JsonGenerator generator, Object value) throws IOException {
        generator.writeStartArray();
        for (Vector vector : (Vector[]) value) {
            VECTOR_TO_JSON_WRITERS[vector.getType().code].write(generator, vector.getValue());
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
            writeVariantValue(generator, entry.getValue());
        }
        generator.writeEndObject();
    }

    private static void writeContainerVector(JsonGenerator generator, Object value) throws IOException {
        generator.writeStartArray();

        for (Container container : (Container[]) value) {
            writeContainer(generator, container);
        }
        generator.writeEndArray();
    }

    private EventToElasticJsonWriter() {
    }
}
