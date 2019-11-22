package ru.kontur.vostok.hercules.protocol;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * @author Gregory Koshelev
 */
public final class Vector {
    private final Type type;
    private final Object value;
    private final int size;

    private Vector(Type type, Object value, int sizeOfValue) {
        this.type = type;
        this.value = value;
        this.size = Sizes.sizeOfType() + Sizes.sizeOfVectorLength() + sizeOfValue;
    }

    public Type getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    public int sizeOf() {
        return size;
    }

    public static Vector ofContainers(Container... containers) {
        return new Vector(
                Type.CONTAINER,
                containers,
                Sizes.sizeOf(containers));
    }

    public static Vector ofBytes(byte... bytes) {
        return new Vector(Type.BYTE, bytes, Sizes.sizeOf(bytes));
    }

    public static Vector ofShorts(short... shorts) {
        return new Vector(Type.SHORT, shorts, Sizes.sizeOf(shorts));
    }

    public static Vector ofIntegers(int... ints) {
        return new Vector(Type.INTEGER, ints, Sizes.sizeOf(ints));
    }

    public static Vector ofLongs(long... longs) {
        return new Vector(Type.LONG, longs, Sizes.sizeOf(longs));
    }

    public static Vector ofFloats(float... floats) {
        return new Vector(Type.FLOAT, floats, Sizes.sizeOf(floats));
    }

    public static Vector ofDoubles(double... doubles) {
        return new Vector(Type.DOUBLE, doubles, Sizes.sizeOf(doubles));
    }

    public static Vector ofFlags(boolean... booleans) {
        return new Vector(Type.FLAG, booleans, Sizes.sizeOf(booleans));
    }

    public static Vector ofStrings(String... strings) {
        byte[][] bytes = new byte[strings.length][];
        for (int i = 0; i < strings.length; ++i) {
            bytes[i] = strings[i].getBytes(StandardCharsets.UTF_8);
        }
        return Vector.ofStringsAsBytes(bytes);
    }

    public static Vector ofStringsAsBytes(byte[]... bytes) {
        return new Vector(Type.STRING, bytes, Sizes.sizeOfByteStrings(bytes));
    }

    public static Vector ofUuids(UUID... uuids) {
        return new Vector(Type.UUID, uuids, Sizes.sizeOf(uuids));
    }

    public static Vector ofNulls(Object... nulls) {
        return new Vector(Type.NULL, nulls, Sizes.sizeOfNulls());
    }

    public static Vector ofVectors(Vector... vectors) {
        return new Vector(Type.VECTOR, vectors, Sizes.sizeOf(vectors));
    }
}
