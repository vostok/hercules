package ru.kontur.vostok.hercules.protocol.encoder;

import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;

import java.util.Arrays;

public class VariantWriter implements Writer<Variant> {
    
    @FunctionalInterface
    private interface VariantValueWriter {
        void write(Encoder encoder, Object value);
    }
    
    private static final VariantValueWriter[] writers = new VariantValueWriter[256];
    static {
        Arrays.setAll(writers, idx -> (e, v) -> {throw new IllegalArgumentException("Unsupported type with code " + idx);});
        
        writers[Type.BYTE.value] = VariantWriter::writeByte;
        writers[Type.SHORT.value] = VariantWriter::writeShort;
        writers[Type.INTEGER.value] = VariantWriter::writeInteger;
        writers[Type.LONG.value] = VariantWriter::writeLong;
        writers[Type.FLAG.value] = VariantWriter::writeFlag;
        writers[Type.FLOAT.value] = VariantWriter::writeFloat;
        writers[Type.DOUBLE.value] = VariantWriter::writeDouble;
        writers[Type.STRING.value] = VariantWriter::writeString;
        writers[Type.TEXT.value] = VariantWriter::writeText;

        writers[Type.BYTE_VECTOR.value] = VariantWriter::writeByteVector;
        writers[Type.SHORT_VECTOR.value] = VariantWriter::writeShortVector;
        writers[Type.INTEGER_VECTOR.value] = VariantWriter::writeIntegerVector;
        writers[Type.LONG_VECTOR.value] = VariantWriter::writeLongVector;
        writers[Type.FLAG_VECTOR.value] = VariantWriter::writeFlagVector;
        writers[Type.FLOAT_VECTOR.value] = VariantWriter::writeFloatVector;
        writers[Type.DOUBLE_VECTOR.value] = VariantWriter::writeDoubleVector;
        writers[Type.STRING_VECTOR.value] = VariantWriter::writeStringVector;
        writers[Type.TEXT_VECTOR.value] = VariantWriter::writeTextVector;

        writers[Type.BYTE_ARRAY.value] = VariantWriter::writeByteArray;
        writers[Type.SHORT_ARRAY.value] = VariantWriter::writeShortArray;
        writers[Type.INTEGER_ARRAY.value] = VariantWriter::writeIntegerArray;
        writers[Type.LONG_ARRAY.value] = VariantWriter::writeLongArray;
        writers[Type.FLAG_ARRAY.value] = VariantWriter::writeFlagArray;
        writers[Type.FLOAT_ARRAY.value] = VariantWriter::writeFloatArray;
        writers[Type.DOUBLE_ARRAY.value] = VariantWriter::writeDoubleArray;
        writers[Type.STRING_ARRAY.value] = VariantWriter::writeStringArray;
        writers[Type.TEXT_ARRAY.value] = VariantWriter::writeTextArray;
    }

    @Override
    public void write(Encoder encoder, Variant variant) {
        encoder.writeByte(variant.getType().value);
        writers[variant.getType().value].write(encoder, variant.getValue());
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
}
