package ru.kontur.vostok.hercules.protocol;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class Variant {

    private final Type type;
    private final Object value;

    public Variant(Type type, Object value) {
        this.type = type;
        this.value = value;
    }

    public Type getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    public static Variant ofContainer(Container container) {
        return new Variant(Type.CONTAINER, container);
    }

    public static Variant ofByte(byte b) {
        return new Variant(Type.BYTE, b);
    }

    public static Variant ofShort(short s) {
        return new Variant(Type.SHORT, s);
    }

    public static Variant ofInteger(int i) {
        return new Variant(Type.INTEGER, i);
    }

    public static Variant ofLong(long l) {
        return new Variant(Type.LONG, l);
    }

    public static Variant ofFloat(float f) {
        return new Variant(Type.FLOAT, f);
    }

    public static Variant ofDouble(double d) {
        return new Variant(Type.DOUBLE, d);
    }

    public static Variant ofFlag(boolean b) {
        return new Variant(Type.FLAG, b);
    }

    public static Variant ofString(String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        return new Variant(Type.STRING, bytes);
    }

    /**
     * New variant of type {@link Type#STRING} from UTF-8 bytes.
     * <p>
     * Passed byte array is used internally in the created variant.
     *
     * @param bytes UTF-8 bytes
     * @return Variant of type {@link Type#STRING}
     */
    public static Variant ofString(final byte[] bytes) {
        return new Variant(Type.STRING, bytes);
    }

    public static Variant ofUuid(UUID uuid) {
        return new Variant(Type.UUID, uuid);
    }

    public static Variant ofNull() {
        return new Variant(Type.NULL, null);
    }

    public static Variant ofVector(Vector v) {
        return new Variant(Type.VECTOR, v);
    }

    @Override
    public String toString() {
        final String stringValue;
        if (Type.STRING == type) {
            stringValue = new String((byte[]) value, StandardCharsets.UTF_8);
        } else {
            stringValue = String.valueOf(value);
        }
        return String.format("(%s) %s", type.name(), stringValue);
    }
}
