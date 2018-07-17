package ru.kontur.vostok.hercules.protocol;

import java.nio.charset.StandardCharsets;

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

    /**
     * Create variant of container
     * @param container which must be wrapped
     * @return specific variant
     */
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
        checkStringBytesLength(bytes);
        return new Variant(Type.STRING, bytes);
    }

    public static Variant ofText(String s) {
        return new Variant(Type.TEXT, s.getBytes(StandardCharsets.UTF_8));
    }

    public static Variant ofByteVector(byte[] bytes) {
        checkVectorLength(bytes.length);
        return new Variant(Type.BYTE_VECTOR, bytes);
    }

    public static Variant ofShortVector(short[] shorts) {
        checkVectorLength(shorts.length);
        return new Variant(Type.SHORT_VECTOR, shorts);
    }

    public static Variant ofIntegerVector(int[] ints) {
        checkVectorLength(ints.length);
        return new Variant(Type.INTEGER_VECTOR, ints);
    }

    public static Variant ofLongVector(long[] longs) {
        checkVectorLength(longs.length);
        return new Variant(Type.LONG_VECTOR, longs);
    }

    public static Variant ofFloatVector(float[] floats) {
        checkVectorLength(floats.length);
        return new Variant(Type.FLOAT_VECTOR, floats);
    }

    public static Variant ofDoubleVector(double[] doubles) {
        checkVectorLength(doubles.length);
        return new Variant(Type.DOUBLE_VECTOR, doubles);
    }

    public static Variant ofFlagVector(boolean[] booleans) {
        checkVectorLength(booleans.length);
        return new Variant(Type.FLAG_VECTOR, booleans);
    }

    public static Variant ofStringVector(String[] strings) {
        checkVectorLength(strings.length);
        byte[][] bytes = new byte[strings.length][];
        for (int i = 0; i < strings.length; ++i) {
            try {
                byte[] stringBytes = strings[i].getBytes(StandardCharsets.UTF_8);
                checkStringBytesLength(stringBytes);
                bytes[i] = stringBytes;
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(String.format(VectorConstants.STRING_LIST_ERROR_MESSAGE_TEMPLATE, i));
            }
        }
        return new Variant(Type.STRING_VECTOR, bytes);
    }

    public static Variant ofTextVector(String[] texts) {
        checkVectorLength(texts.length);
        byte[][] bytes = new byte[texts.length][];
        for (int i = 0; i < texts.length; ++i) {
            bytes[i] = texts[i].getBytes(StandardCharsets.UTF_8);
        }
        return new Variant(Type.TEXT_VECTOR, bytes);
    }

    /**
     * Create variant of containers
     * @param containers which must be wrapped
     * @return specific variant
     */
    public static Variant ofContainerVector(Container[] containers) {
        checkVectorLength(containers.length);
        return new Variant(Type.CONTAINER_VECTOR, containers);
    }

    public static Variant ofByteArray(byte[] bytes) {
        return new Variant(Type.BYTE_ARRAY, bytes);
    }

    public static Variant ofShortArray(short[] shorts) {
        return new Variant(Type.SHORT_ARRAY, shorts);
    }

    public static Variant ofIntegerArray(int[] ints) {
        return new Variant(Type.INTEGER_ARRAY, ints);
    }

    public static Variant ofLongArray(long[] longs) {
        return new Variant(Type.LONG_ARRAY, longs);
    }

    public static Variant ofFloatArray(float[] floats) {
        return new Variant(Type.FLOAT_ARRAY, floats);
    }

    public static Variant ofDoubleArray(double[] doubles) {
        return new Variant(Type.DOUBLE_ARRAY, doubles);
    }

    public static Variant ofFlagArray(boolean[] booleans) {
        return new Variant(Type.FLAG_ARRAY, booleans);
    }

    public static Variant ofStringArray(String[] strings) {
        byte[][] bytes = new byte[strings.length][];
        for (int i = 0; i < strings.length; ++i) {
            try {
                byte[] stringBytes = strings[i].getBytes(StandardCharsets.UTF_8);
                checkStringBytesLength(stringBytes);
                bytes[i] = stringBytes;
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(String.format(VectorConstants.STRING_LIST_ERROR_MESSAGE_TEMPLATE, i));
            }
        }
        return new Variant(Type.STRING_ARRAY, bytes);
    }

    public static Variant ofTextArray(String[] texts) {
        byte[][] bytes = new byte[texts.length][];
        for (int i = 0; i < texts.length; ++i) {
            bytes[i] = texts[i].getBytes(StandardCharsets.UTF_8);
        }
        return new Variant(Type.TEXT_ARRAY, bytes);
    }

    /**
     * Create variant of containers
     * @param containers which must be wrapped
     * @return specific variant
     */
    public static Variant ofContainerArray(Container[] containers) {
        return new Variant(Type.CONTAINER_ARRAY, containers);
    }

    private static void checkVectorLength(int length) {
        if (VectorConstants.VECTOR_LENGTH_EXCEEDED <= length) {
            throw new IllegalArgumentException(VectorConstants.VECTOR_LENGTH_ERROR_MESSAGE);
        }
    }

    private static void checkStringBytesLength(byte[] bytes) {
        if (VectorConstants.VECTOR_LENGTH_EXCEEDED <= bytes.length) {
            throw new IllegalArgumentException(VectorConstants.STRING_LENGTH_ERROR_MESSAGE);
        }
    }
}
