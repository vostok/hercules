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
            case STRING: encoder.writeString((String) value); break;
            case TEXT: encoder.writeText((String) value); break;

            case RESERVED: throw typeNotSupported(type);
            default: throw typeNotSupported(type);
            // TODO: Make the rest of table and unit tests
        }
    }

    private static IllegalArgumentException typeNotSupported(Type type) {
        return new IllegalArgumentException(String.format("Type '%s' is not supported", type.name()));
    }
}
