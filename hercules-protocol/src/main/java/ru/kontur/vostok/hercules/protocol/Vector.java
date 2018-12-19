package ru.kontur.vostok.hercules.protocol;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * @author Gregory Koshelev
 */
public final class Vector {
    private final Type type;
    private final Object value;

    public Vector(Type type, Object value) {
        this.type = type;
        this.value = value;
    }

    public Type getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    public static Vector ofContainers(Container[] containers) {
        return new Vector(Type.CONTAINER, containers);
    }

    public static Vector ofBytes(byte[] bytes) {
        return new Vector(Type.BYTE, bytes);
    }

    public static Vector ofShorts(short[] shorts) {
        return new Vector(Type.SHORT, shorts);
    }

    public static Vector ofIntegers(int[] ints) {
        return new Vector(Type.INTEGER, ints);
    }

    public static Vector ofLongs(long[] longs) {
        return new Vector(Type.LONG, longs);
    }

    public static Vector ofFloats(float[] floats) {
        return new Vector(Type.FLOAT, floats);
    }

    public static Vector ofDoubles(double[] doubles) {
        return new Vector(Type.DOUBLE, doubles);
    }

    public static Vector ofFlags(boolean[] booleans) {
        return new Vector(Type.FLAG, booleans);
    }

    public static Vector ofStrings(String[] strings) {
        byte[][] bytes = new byte[strings.length][];
        for (int i = 0; i < strings.length; ++i) {
            bytes[i] = strings[i].getBytes(StandardCharsets.UTF_8);
        }
        return new Vector(Type.STRING, bytes);
    }

    public static Vector ofUuids(UUID[] uuids) {
        return new Vector(Type.UUID, uuids);
    }

    public static Vector ofNulls(Object[] nulls) {
        return new Vector(Type.NULL, nulls);
    }

    public static Vector ofVectors(Vector[] vectors) {
        return new Vector(Type.VECTOR, vectors);
    }
}
