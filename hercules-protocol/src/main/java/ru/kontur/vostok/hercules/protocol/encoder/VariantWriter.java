package ru.kontur.vostok.hercules.protocol.encoder;

import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;


public class VariantWriter {

    public static void write(Encoder encoder, Variant variant) {

        Type type = variant.getType();
        Object value = variant.getValue();

        encoder.writeByte(type.value);
        switch (type) {
            case BYTE: encoder.writeByte((byte) value); break;
            case SHORT: encoder.writeShort((short) value); break;
            case INTEGER: encoder.writeInteger((int) value); break;
            case LONG: encoder.writeLong((long) value); break;
            case FLAG: encoder.writeFlag((boolean) value); break;
            case FLOAT: encoder.writeFloat((float) value); break;
            case DOUBLE: encoder.writeDouble((double) value); break;
            case STRING: encoder.writeBytesAsString((byte[]) value); break;
            case TEXT: encoder.writeBytesAsText((byte[]) value); break;

            case BYTE_VECTOR: encoder.writeByteVector((byte[]) value); break;
            case SHORT_VECTOR: encoder.writeShortVector((short[]) value); break;
            case INTEGER_VECTOR: encoder.writeIntegerVector((int[]) value); break;
            case LONG_VECTOR: encoder.writeLongVector((long[]) value); break;
            case FLOAT_VECTOR: encoder.writeFloatVector((float[]) value); break;
            case DOUBLE_VECTOR: encoder.writeDoubleVector((double[]) value); break;
            case FLAG_VECTOR: encoder.writeFlagVector((boolean[]) value); break;
            case STRING_VECTOR: encoder.writeBytesAsStringVector((byte[][]) value); break;
            case TEXT_VECTOR: encoder.writeBytesAsTextVector((byte[][]) value); break;

            case BYTE_ARRAY: encoder.writeByteArray((byte[]) value); break;
            case SHORT_ARRAY: encoder.writeShortArray((short[]) value); break;
            case INTEGER_ARRAY: encoder.writeIntegerArray((int[]) value); break;
            case LONG_ARRAY: encoder.writeLongArray((long[]) value); break;
            case FLOAT_ARRAY: encoder.writeFloatArray((float[]) value); break;
            case DOUBLE_ARRAY: encoder.writeDoubleArray((double[]) value); break;
            case FLAG_ARRAY: encoder.writeFlagArray((boolean[]) value); break;
            case STRING_ARRAY: encoder.writeBytesAsStringArray((byte[][]) value); break;
            case TEXT_ARRAY: encoder.writeBytesAsTextArray((byte[][]) value); break;

            case RESERVED: throw typeNotSupported(type);
            default: throw typeNotSupported(type);
        }
    }

    private static IllegalArgumentException typeNotSupported(Type type) {
        return new IllegalArgumentException(String.format("Type '%s' is not supported", type.name()));
    }
}
