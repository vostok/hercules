package ru.kontur.vostok.hercules.protocol.encoder;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.Vector;

import java.util.Arrays;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Hercules Protocol Writer for any variant data
 */
public class VariantWriter implements Writer<Variant> {

    public static final VariantWriter INSTANCE = new VariantWriter();
    public static final ContainerWriter CONTAINER_WRITER = ContainerWriter.INSTANCE;
    public static final ContainerVectorWriter CONTAINER_VECTOR_WRITER = ContainerVectorWriter.INSTANCE;
    private static final ObjectWriter[] WRITERS = new ObjectWriter[256];
    private static final ObjectWriter[] VECTOR_WRITERS = new ObjectWriter[256];

    static {
        Arrays.setAll(WRITERS, idx -> (e, v) -> {
            throw new IllegalArgumentException("Unsupported type with code " + idx);
        });

        WRITERS[Type.CONTAINER.code] = VariantWriter::writeContainer;
        WRITERS[Type.BYTE.code] = VariantWriter::writeByte;
        WRITERS[Type.SHORT.code] = VariantWriter::writeShort;
        WRITERS[Type.INTEGER.code] = VariantWriter::writeInteger;
        WRITERS[Type.LONG.code] = VariantWriter::writeLong;
        WRITERS[Type.FLAG.code] = VariantWriter::writeFlag;
        WRITERS[Type.FLOAT.code] = VariantWriter::writeFloat;
        WRITERS[Type.DOUBLE.code] = VariantWriter::writeDouble;
        WRITERS[Type.STRING.code] = VariantWriter::writeString;
        WRITERS[Type.UUID.code] = VariantWriter::writeUuid;
        WRITERS[Type.NULL.code] = VariantWriter::writeNull;
        WRITERS[Type.VECTOR.code] = VariantWriter::writeVector;
    }

    static {
        Arrays.setAll(VECTOR_WRITERS, idx -> (e, v) -> {
            throw new IllegalArgumentException("Unsupported type with code " + idx);
        });

        VECTOR_WRITERS[Type.CONTAINER.code] = VariantWriter::writeContainerVector;
        VECTOR_WRITERS[Type.BYTE.code] = VariantWriter::writeByteVector;
        VECTOR_WRITERS[Type.SHORT.code] = VariantWriter::writeShortVector;
        VECTOR_WRITERS[Type.INTEGER.code] = VariantWriter::writeIntegerVector;
        VECTOR_WRITERS[Type.LONG.code] = VariantWriter::writeLongVector;
        VECTOR_WRITERS[Type.FLAG.code] = VariantWriter::writeFlagVector;
        VECTOR_WRITERS[Type.FLOAT.code] = VariantWriter::writeFloatVector;
        VECTOR_WRITERS[Type.DOUBLE.code] = VariantWriter::writeDoubleVector;
        VECTOR_WRITERS[Type.STRING.code] = VariantWriter::writeStringVector;
        VECTOR_WRITERS[Type.UUID.code] = VariantWriter::writeUuidVector;
        VECTOR_WRITERS[Type.NULL.code] = VariantWriter::writeNullVector;
        VECTOR_WRITERS[Type.VECTOR.code] = VariantWriter::writeVectorOfVectors;
    }

    private static void writeContainer(Encoder encoder, Object value) {
        Container container = (Container) value;
        CONTAINER_WRITER.write(encoder, container);
    }

    private static void writeByte(Encoder encoder, Object value) {
        encoder.writeByte((byte) value);
    }

    private static void writeShort(Encoder encoder, Object value) {
        encoder.writeShort((short) value);
    }

    private static void writeInteger(Encoder encoder, Object value) {
        encoder.writeInteger((int) value);
    }

    private static void writeLong(Encoder encoder, Object value) {
        encoder.writeLong((long) value);
    }

    private static void writeFlag(Encoder encoder, Object value) {
        encoder.writeFlag((boolean) value);
    }

    private static void writeFloat(Encoder encoder, Object value) {
        encoder.writeFloat((float) value);
    }

    private static void writeDouble(Encoder encoder, Object value) {
        encoder.writeDouble((double) value);
    }

    private static void writeString(Encoder encoder, Object value) {
        encoder.writeBytesAsString((byte[]) value);
    }

    private static void writeUuid(Encoder encoder, Object value) {
        encoder.writeUuid((UUID) value);
    }

    private static void writeNull(Encoder encoder, Object value) {
        encoder.writeNull();
    }

    private static void writeVector(Encoder encoder, Object value) {
        Vector vector = (Vector) value;
        Type type = vector.getType();
        encoder.writeType(type);
        VECTOR_WRITERS[type.code].accept(encoder, vector.getValue());
    }

    private static void writeContainerVector(Encoder encoder, Object value) {
        Container[] containers = (Container[]) value;

        CONTAINER_VECTOR_WRITER.write(encoder, containers);
    }

    private static void writeByteVector(Encoder encoder, Object value) {
        encoder.writeByteVector((byte[]) value);
    }

    private static void writeShortVector(Encoder encoder, Object value) {
        encoder.writeShortVector((short[]) value);
    }

    private static void writeIntegerVector(Encoder encoder, Object value) {
        encoder.writeIntegerVector((int[]) value);
    }

    private static void writeLongVector(Encoder encoder, Object value) {
        encoder.writeLongVector((long[]) value);
    }

    private static void writeFloatVector(Encoder encoder, Object value) {
        encoder.writeFloatVector((float[]) value);
    }

    private static void writeDoubleVector(Encoder encoder, Object value) {
        encoder.writeDoubleVector((double[]) value);
    }

    private static void writeFlagVector(Encoder encoder, Object value) {
        encoder.writeFlagVector((boolean[]) value);
    }

    private static void writeStringVector(Encoder encoder, Object value) {
        encoder.writeBytesAsStringVector((byte[][]) value);
    }

    private static void writeUuidVector(Encoder encoder, Object value) {
        encoder.writeUuidVector((UUID[]) value);
    }

    private static void writeNullVector(Encoder encoder, Object value) {
        encoder.writeNullVector((Object[]) value);
    }

    private static void writeVectorOfVectors(Encoder encoder, Object value) {
        Vector[] vectors = (Vector[]) value;
        encoder.writeVectorLength(vectors.length);
        for (Vector vector : vectors) {
            writeVector(encoder, vector);
        }
    }

    /**
     * Hercules Protocol Write variant with encoder
     *
     * @param encoder Encoder for write data
     * @param variant Variant which must be written
     */
    @Override
    public void write(Encoder encoder, Variant variant) {
        encoder.writeType(variant.getType());
        WRITERS[variant.getType().code].accept(encoder, variant.getValue());
    }

    private interface ObjectWriter extends BiConsumer<Encoder, Object> {
    }
}
