package ru.kontur.vostok.hercules.json;

import com.fasterxml.jackson.core.JsonGenerator;
import ru.kontur.vostok.hercules.json.format.EventJsonFormatter;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.TinyString;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.Vector;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

/**
 * EventToJsonWriter
 *
 * @author Kirill Sulim
 * @deprecated Use {@link EventJsonFormatter} and {@link DocumentWriter}
 */
@Deprecated
public final class EventToJsonWriter {

    @FunctionalInterface
    public interface VariantValueToJsonWriter {
        void write(JsonGenerator generator, Object value) throws IOException;
    }

    public static final VariantValueToJsonWriter[] TO_JSON_WRITERS = new VariantValueToJsonWriter[256];
    public static final VariantValueToJsonWriter[] VECTOR_TO_JSON_WRITERS = new VariantValueToJsonWriter[256];

    static {
        Arrays.setAll(EventToJsonWriter.TO_JSON_WRITERS, idx -> (g, v) -> {
            throw new IllegalArgumentException("Not implemented for index " + idx);
        });

        EventToJsonWriter.TO_JSON_WRITERS[Type.CONTAINER.code] = EventToJsonWriter::writeContainer;
        EventToJsonWriter.TO_JSON_WRITERS[Type.BYTE.code] = EventToJsonWriter::writeByte;
        EventToJsonWriter.TO_JSON_WRITERS[Type.SHORT.code] = EventToJsonWriter::writeShort;
        EventToJsonWriter.TO_JSON_WRITERS[Type.INTEGER.code] = EventToJsonWriter::writeInteger;
        EventToJsonWriter.TO_JSON_WRITERS[Type.LONG.code] = EventToJsonWriter::writeLong;
        EventToJsonWriter.TO_JSON_WRITERS[Type.FLAG.code] = EventToJsonWriter::writeFlag;
        EventToJsonWriter.TO_JSON_WRITERS[Type.FLOAT.code] = EventToJsonWriter::writeFloat;
        EventToJsonWriter.TO_JSON_WRITERS[Type.DOUBLE.code] = EventToJsonWriter::writeDouble;
        EventToJsonWriter.TO_JSON_WRITERS[Type.STRING.code] = EventToJsonWriter::writeString;
        EventToJsonWriter.TO_JSON_WRITERS[Type.UUID.code] = EventToJsonWriter::writeUuid;
        EventToJsonWriter.TO_JSON_WRITERS[Type.NULL.code] = EventToJsonWriter::writeAsNull;
        EventToJsonWriter.TO_JSON_WRITERS[Type.VECTOR.code] = EventToJsonWriter::writeVector;
    }

    static {
        Arrays.setAll(EventToJsonWriter.VECTOR_TO_JSON_WRITERS, idx -> (g, v) -> {
            throw new IllegalArgumentException("Not implemented for index " + idx);
        });

        EventToJsonWriter.VECTOR_TO_JSON_WRITERS[Type.CONTAINER.code] = EventToJsonWriter::writeContainerVector;
        EventToJsonWriter.VECTOR_TO_JSON_WRITERS[Type.BYTE.code] = EventToJsonWriter::writeByteVector;
        EventToJsonWriter.VECTOR_TO_JSON_WRITERS[Type.SHORT.code] = EventToJsonWriter::writeShortVector;
        EventToJsonWriter.VECTOR_TO_JSON_WRITERS[Type.INTEGER.code] = EventToJsonWriter::writeIntegerVector;
        EventToJsonWriter.VECTOR_TO_JSON_WRITERS[Type.LONG.code] = EventToJsonWriter::writeLongVector;
        EventToJsonWriter.VECTOR_TO_JSON_WRITERS[Type.FLOAT.code] = EventToJsonWriter::writeFloatVector;
        EventToJsonWriter.VECTOR_TO_JSON_WRITERS[Type.DOUBLE.code] = EventToJsonWriter::writeDoubleVector;
        EventToJsonWriter.VECTOR_TO_JSON_WRITERS[Type.FLAG.code] = EventToJsonWriter::writeFlagVector;
        EventToJsonWriter.VECTOR_TO_JSON_WRITERS[Type.STRING.code] = EventToJsonWriter::writeStringVector;
        EventToJsonWriter.VECTOR_TO_JSON_WRITERS[Type.UUID.code] = EventToJsonWriter::writeUuidVector;
        EventToJsonWriter.VECTOR_TO_JSON_WRITERS[Type.NULL.code] = EventToJsonWriter::writeNullVector;
        EventToJsonWriter.VECTOR_TO_JSON_WRITERS[Type.VECTOR.code] = EventToJsonWriter::writeVectorVector;
    }

    public static void writeVariantAsField(JsonGenerator generator, String tagName, Variant variant) throws IOException {
        generator.writeFieldName(tagName);
        writeVariantValue(generator, variant);
    }

    public static void writeVariantValue(JsonGenerator generator, Variant variant) throws IOException {
        TO_JSON_WRITERS[variant.getType().code].write(generator, variant.getValue());
    }

    public static void writeByte(JsonGenerator generator, Object value) throws IOException {
        generator.writeNumber((byte) value);
    }

    public static void writeShort(JsonGenerator generator, Object value) throws IOException {
        generator.writeNumber((short) value);
    }

    public static void writeInteger(JsonGenerator generator, Object value) throws IOException {
        generator.writeNumber((int) value);
    }

    public static void writeLong(JsonGenerator generator, Object value) throws IOException {
        generator.writeNumber((long) value);
    }

    public static void writeFloat(JsonGenerator generator, Object value) throws IOException {
        generator.writeNumber((float) value);
    }

    public static void writeDouble(JsonGenerator generator, Object value) throws IOException {
        generator.writeNumber((double) value);
    }

    public static void writeFlag(JsonGenerator generator, Object value) throws IOException {
        generator.writeBoolean((boolean) value);
    }

    public static void writeString(JsonGenerator generator, Object value) throws IOException {
        generator.writeString(new String((byte[]) value, StandardCharsets.UTF_8));
    }

    public static void writeUuid(JsonGenerator generator, Object value) throws IOException {
        generator.writeString(((UUID)value).toString());
    }

    public static void writeVector(JsonGenerator generator, Object value) throws IOException {
        Vector vector = (Vector) value;
        VECTOR_TO_JSON_WRITERS[vector.getType().code].write(generator, vector.getValue());
    }

    public static void writeByteVector(JsonGenerator generator, Object value) throws IOException {
        generator.writeStartArray();
        for (byte b : (byte[]) value) {
            generator.writeNumber(b);
        }
        generator.writeEndArray();
    }

    public static void writeShortVector(JsonGenerator generator, Object value) throws IOException {
        generator.writeStartArray();
        for (short s : (short[]) value) {
            generator.writeNumber(s);
        }
        generator.writeEndArray();
    }

    public static void writeIntegerVector(JsonGenerator generator, Object value) throws IOException {
        generator.writeStartArray();
        for (int i : (int[]) value) {
            generator.writeNumber(i);
        }
        generator.writeEndArray();
    }

    public static void writeLongVector(JsonGenerator generator, Object value) throws IOException {
        generator.writeStartArray();
        for (long l : (long[]) value) {
            generator.writeNumber(l);
        }
        generator.writeEndArray();
    }

    public static void writeFloatVector(JsonGenerator generator, Object value) throws IOException {
        generator.writeStartArray();
        for (float f : (float[]) value) {
            generator.writeNumber(f);
        }
        generator.writeEndArray();
    }

    public static void writeDoubleVector(JsonGenerator generator, Object value) throws IOException {
        generator.writeStartArray();
        for (double d : (double[]) value) {
            generator.writeNumber(d);
        }
        generator.writeEndArray();
    }

    public static void writeFlagVector(JsonGenerator generator, Object value) throws IOException {
        generator.writeStartArray();
        for (boolean b : (boolean[]) value) {
            generator.writeBoolean(b);
        }
        generator.writeEndArray();
    }

    public static void writeStringVector(JsonGenerator generator, Object value) throws IOException {
        generator.writeStartArray();
        for (byte[] bytes : (byte[][]) value) {
            generator.writeString(new String(bytes, StandardCharsets.UTF_8));
        }
        generator.writeEndArray();
    }

    public static void writeUuidVector(JsonGenerator generator, Object value) throws IOException {
        generator.writeStartArray();
        for (UUID uuid : (UUID[]) value) {
            generator.writeString(uuid.toString());
        }
        generator.writeEndArray();
    }

    public static void writeNullVector(JsonGenerator generator, Object value) throws IOException {
        generator.writeStartArray();
        Object[] nulls = (Object[]) value;
        for (int i = 0; i < nulls.length; i++) {
            generator.writeNull();
        }
        generator.writeEndArray();
    }

    public static void writeVectorVector(JsonGenerator generator, Object value) throws IOException {
        generator.writeStartArray();
        for (Vector vector : (Vector[]) value) {
            VECTOR_TO_JSON_WRITERS[vector.getType().code].write(generator, vector.getValue());
        }
        generator.writeEndArray();
    }

    public static void writeAsNull(JsonGenerator generator, Object value) throws IOException {
        generator.writeNull();
    }

    public static void writeContainer(JsonGenerator generator, Object value) throws IOException {
        generator.writeStartObject();
        for (Map.Entry<TinyString, Variant> entry : ((Container)value).tags().entrySet()) {
            generator.writeFieldName(entry.getKey().toString());
            writeVariantValue(generator, entry.getValue());
        }
        generator.writeEndObject();
    }

    public static void writeContainerVector(JsonGenerator generator, Object value) throws IOException {
        generator.writeStartArray();

        for (Container container : (Container[]) value) {
            writeContainer(generator, container);
        }
        generator.writeEndArray();
    }

    private EventToJsonWriter() {
        /* static class */
    }
}
