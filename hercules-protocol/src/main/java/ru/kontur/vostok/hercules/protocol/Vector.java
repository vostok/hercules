package ru.kontur.vostok.hercules.protocol;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * @author Gregory Koshelev
 */
public final class Vector {
    private static final int SIZE_OF_VECTOR_LENGTH = Type.INTEGER.size;

    private final Type type;
    private final Object value;
    private final int size;

    private Vector(Type type, Object value, int size) {
        this.type = type;
        this.value = value;
        this.size = size;
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
        return new Vector(Type.CONTAINER, containers, sizeOfVector(containers));
    }

    public static Vector ofBytes(byte... bytes) {
        return new Vector(Type.BYTE, bytes, sizeOfVector(bytes));
    }

    public static Vector ofShorts(short... shorts) {
        return new Vector(Type.SHORT, shorts, sizeOfVector(shorts));
    }

    public static Vector ofIntegers(int... ints) {
        return new Vector(Type.INTEGER, ints, sizeOfVector(ints));
    }

    public static Vector ofLongs(long... longs) {
        return new Vector(Type.LONG, longs, sizeOfVector(longs));
    }

    public static Vector ofFloats(float... floats) {
        return new Vector(Type.FLOAT, floats, sizeOfVector(floats));
    }

    public static Vector ofDoubles(double... doubles) {
        return new Vector(Type.DOUBLE, doubles, sizeOfVector(doubles));
    }

    public static Vector ofFlags(boolean... booleans) {
        return new Vector(Type.FLAG, booleans, sizeOfVector(booleans));
    }

    public static Vector ofStrings(String... strings) {
        byte[][] bytes = new byte[strings.length][];
        for (int i = 0; i < strings.length; ++i) {
            bytes[i] = strings[i].getBytes(StandardCharsets.UTF_8);
        }
        return Vector.ofStringsAsBytes(bytes);
    }

    public static Vector ofStringsAsBytes(byte[]... bytes) {
        return new Vector(Type.STRING, bytes, sizeOfStringVector(bytes));
    }

    public static Vector ofUuids(UUID... uuids) {
        return new Vector(Type.UUID, uuids, sizeOfVector(uuids));
    }

    public static Vector ofNulls(Object... nulls) {
        return new Vector(Type.NULL, nulls, sizeOfNullVector());
    }

    public static Vector ofVectors(Vector... vectors) {
        return new Vector(Type.VECTOR, vectors, sizeOfVector(vectors));
    }

    /**
     * Size of vector of containers.
     *
     * @param containers containers
     * @return size of vector of containers
     */
    private static int sizeOfVector(Container[] containers) {
        int size = 0;
        for (Container container : containers) {
            size += container.sizeOf();
        }
        return sizeOfVector(size);
    }

    /**
     * Size of vector of bytes.
     *
     * @param data bytes
     * @return size of vector of bytes
     */
    private static int sizeOfVector(byte[] data) {
        return sizeOfVector(Type.BYTE.size * data.length);
    }

    /**
     * Size of vector of shorts.
     *
     * @param data shorts
     * @return size of vector of shorts
     */
    private static int sizeOfVector(short[] data) {
        return sizeOfVector(Type.SHORT.size * data.length);
    }

    /**
     * Size of vector of integers.
     *
     * @param data bytes
     * @return size of vector of integers
     */
    private static int sizeOfVector(int[] data) {
        return sizeOfVector(Type.INTEGER.size * data.length);
    }

    /**
     * Size of vector of longs.
     *
     * @param data bytes
     * @return size of vector of longs
     */
    private static int sizeOfVector(long[] data) {
        return sizeOfVector(Type.LONG.size * data.length);
    }

    /**
     * Size of vector of flags.
     *
     * @param data bytes
     * @return size of vector of flags
     */
    private static int sizeOfVector(boolean[] data) {
        return sizeOfVector(Type.FLAG.size * data.length);
    }

    /**
     * Size of vector of floats.
     *
     * @param data bytes
     * @return size of vector of floats
     */
    private static int sizeOfVector(float[] data) {
        return sizeOfVector(Type.FLOAT.size * data.length);
    }

    /**
     * Size of vector of doubles.
     *
     * @param data bytes
     * @return size of vector of doubles
     */
    private static int sizeOfVector(double[] data) {
        return sizeOfVector(Type.DOUBLE.size * data.length);
    }

    /**
     * Size of vector of strings.
     *
     * @param data bytes
     * @return size of vector of strings
     */
    private static int sizeOfStringVector(byte[][] data) {
        int size = 0;
        for (byte[] bytes : data) {
            size += Sizes.sizeOfString(bytes);
        }
        return sizeOfVector(size);
    }

    /**
     * Size of vector of UUIDs.
     *
     * @param data bytes
     * @return size of vector of UUIDs
     */
    private static int sizeOfVector(UUID[] data) {
        return sizeOfVector(Type.UUID.size * data.length);
    }

    /**
     * Size of vector of nulls.
     *
     * @return size of vector of nulls
     */
    private static int sizeOfNullVector() {
        return sizeOfVector(0);
    }

    /**
     * Size of vector of vectors.
     *
     * @param data bytes
     * @return size of vector of vectors
     */
    private static int sizeOfVector(Vector[] data) {
        int size = 0;
        for (Vector vector : data) {
            size += vector.sizeOf();
        }
        return sizeOfVector(size);
    }

    /**
     * Size of vector with specified content size.
     *
     * @param contentSize size of vector's content
     * @return size of vector
     */
    private static int sizeOfVector(int contentSize) {
        return Type.TYPE.size + SIZE_OF_VECTOR_LENGTH + contentSize;
    }
}
