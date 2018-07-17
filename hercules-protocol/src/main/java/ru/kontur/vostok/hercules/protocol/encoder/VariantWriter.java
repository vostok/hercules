package ru.kontur.vostok.hercules.protocol.encoder;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;

import java.util.Arrays;
import java.util.function.BiConsumer;

/**
 * Hercules Protocol Writer for any variant data
 */
public class VariantWriter implements Writer<Variant> {

    public static final VariantWriter INSTANCE = new VariantWriter();
    public static final ContainerWriter containerWriter = ContainerWriter.INSTANCE;
    public static final ContainerArrayWriter containerArrayWriter = ContainerArrayWriter.INSTANCE;
    public static final ContainerVectorWriter containerVectorWriter = ContainerVectorWriter.INSTANCE;
    private static final ObjectWriter[] writers = new ObjectWriter[256];

    static {
        Arrays.setAll(writers, idx -> (e, v) -> {
            throw new IllegalArgumentException("Unsupported type with code " + idx);
        });

        writers[Type.CONTAINER.code] = VariantWriter::writeContainer;
        writers[Type.BYTE.code] = VariantWriter::writeByte;
        writers[Type.SHORT.code] = VariantWriter::writeShort;
        writers[Type.INTEGER.code] = VariantWriter::writeInteger;
        writers[Type.LONG.code] = VariantWriter::writeLong;
        writers[Type.FLAG.code] = VariantWriter::writeFlag;
        writers[Type.FLOAT.code] = VariantWriter::writeFloat;
        writers[Type.DOUBLE.code] = VariantWriter::writeDouble;
        writers[Type.STRING.code] = VariantWriter::writeString;
        writers[Type.TEXT.code] = VariantWriter::writeText;

        writers[Type.CONTAINER_VECTOR.code] = VariantWriter::writeContainerVector;
        writers[Type.BYTE_VECTOR.code] = VariantWriter::writeByteVector;
        writers[Type.SHORT_VECTOR.code] = VariantWriter::writeShortVector;
        writers[Type.INTEGER_VECTOR.code] = VariantWriter::writeIntegerVector;
        writers[Type.LONG_VECTOR.code] = VariantWriter::writeLongVector;
        writers[Type.FLAG_VECTOR.code] = VariantWriter::writeFlagVector;
        writers[Type.FLOAT_VECTOR.code] = VariantWriter::writeFloatVector;
        writers[Type.DOUBLE_VECTOR.code] = VariantWriter::writeDoubleVector;
        writers[Type.STRING_VECTOR.code] = VariantWriter::writeStringVector;
        writers[Type.TEXT_VECTOR.code] = VariantWriter::writeTextVector;

        writers[Type.CONTAINER_ARRAY.code] = VariantWriter::writeContainerArray;
        writers[Type.BYTE_ARRAY.code] = VariantWriter::writeByteArray;
        writers[Type.SHORT_ARRAY.code] = VariantWriter::writeShortArray;
        writers[Type.INTEGER_ARRAY.code] = VariantWriter::writeIntegerArray;
        writers[Type.LONG_ARRAY.code] = VariantWriter::writeLongArray;
        writers[Type.FLAG_ARRAY.code] = VariantWriter::writeFlagArray;
        writers[Type.FLOAT_ARRAY.code] = VariantWriter::writeFloatArray;
        writers[Type.DOUBLE_ARRAY.code] = VariantWriter::writeDoubleArray;
        writers[Type.STRING_ARRAY.code] = VariantWriter::writeStringArray;
        writers[Type.TEXT_ARRAY.code] = VariantWriter::writeTextArray;
    }

    private static void writeContainer(Encoder encoder, Object value) {
        Container container = (Container) value;
        containerWriter.write(encoder, container);
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

    private static void writeText(Encoder encoder, Object value) {
        encoder.writeBytesAsText((byte[]) value);
    }

    private static void writeContainerVector(Encoder encoder, Object value) {
        Container[] containers = (Container[]) value;

        containerVectorWriter.write(encoder, containers);
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

    private static void writeTextVector(Encoder encoder, Object value) {
        encoder.writeBytesAsTextVector((byte[][]) value);
    }

    private static void writeContainerArray(Encoder encoder, Object value) {
        Container[] containers = (Container[]) value;

        containerArrayWriter.write(encoder, containers);
    }

    private static void writeByteArray(Encoder encoder, Object value) {
        encoder.writeByteArray((byte[]) value);
    }

    private static void writeShortArray(Encoder encoder, Object value) {
        encoder.writeShortArray((short[]) value);
    }

    private static void writeIntegerArray(Encoder encoder, Object value) {
        encoder.writeIntegerArray((int[]) value);
    }

    private static void writeLongArray(Encoder encoder, Object value) {
        encoder.writeLongArray((long[]) value);
    }

    private static void writeFloatArray(Encoder encoder, Object value) {
        encoder.writeFloatArray((float[]) value);
    }

    private static void writeDoubleArray(Encoder encoder, Object value) {
        encoder.writeDoubleArray((double[]) value);
    }

    private static void writeFlagArray(Encoder encoder, Object value) {
        encoder.writeFlagArray((boolean[]) value);
    }

    private static void writeStringArray(Encoder encoder, Object value) {
        encoder.writeBytesAsStringArray((byte[][]) value);
    }

    private static void writeTextArray(Encoder encoder, Object value) {
        encoder.writeBytesAsTextArray((byte[][]) value);
    }

    /**
     * Hercules Protocol Write variant with encoder
     *
     * @param encoder Encoder for write data
     * @param variant Variant which must be written
     */
    @Override
    public void write(Encoder encoder, Variant variant) {
        encoder.writeByte(variant.getType().code);
        writers[variant.getType().code].accept(encoder, variant.getValue());
    }

    private interface ObjectWriter extends BiConsumer<Encoder, Object> {
    }
}
