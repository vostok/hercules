package ru.kontur.vostok.hercules.protocol;

import java.util.UUID;

/**
 * @author Gregory Koshelev
 */
public class Sizes {
    public static final int SIZE_OF_BYTE = 1;
    public static final int SIZE_OF_SHORT = 2;
    public static final int SIZE_OF_INTEGER = 4;
    public static final int SIZE_OF_LONG = 8;
    public static final int SIZE_OF_FLAG = 1;
    public static final int SIZE_OF_FLOAT = 4;
    public static final int SIZE_OF_DOUBLE = 8;
    public static final int SIZE_OF_UUID = 16;
    public static final int SIZE_OF_TYPE = 1;

    public static int sizeOfVersion() {
        return SIZE_OF_BYTE;
    }

    public static int sizeOfTimestamp() {
        return SIZE_OF_LONG;
    }

    public static int sizeOfType() {
        return SIZE_OF_TYPE;
    }

    public static int sizeOfTinyStringLength() {
        return SIZE_OF_BYTE;
    }

    public static int sizeOfStringLength() {
        return SIZE_OF_INTEGER;
    }

    public static int sizeOfTinyString(TinyString tinyString) {
        return sizeOfTinyStringLength() + tinyString.size();
    }

    public static int sizeOfVariant(Variant value) {
        return sizeOfType() + sizeOfVariantValue(value);
    }

    public static int sizeOfVariantValue(Variant variant) {
        switch (variant.getType()) {
            case CONTAINER:
                return ((Container) variant.getValue()).sizeOf();
            case BYTE:
                return SIZE_OF_BYTE;
            case SHORT:
                return SIZE_OF_SHORT;
            case INTEGER:
                return SIZE_OF_INTEGER;
            case LONG:
                return SIZE_OF_LONG;
            case FLAG:
                return SIZE_OF_FLAG;
            case FLOAT:
                return SIZE_OF_FLOAT;
            case DOUBLE:
                return SIZE_OF_DOUBLE;
            case STRING:
                return SIZE_OF_INTEGER + ((byte[]) variant.getValue()).length;
            case UUID:
                return SIZE_OF_UUID;
            case NULL:
                return 0;
            case VECTOR:
                return ((Vector)variant.getValue()).sizeOf();
            default:
                return 0;
        }
    }

    public static int sizeOfTagCount() {
        return SIZE_OF_BYTE * 2;
    }

    public static int sizeOfVectorLength() {
        return SIZE_OF_INTEGER;
    }

    public static int sizeOf(Container[] containers) {
        int size = 0;
        for (Container container : containers) {
            size += container.sizeOf();
        }
        return size;
    }

    public static int sizeOf(byte[] data) {
        return SIZE_OF_BYTE * data.length;
    }

    public static int sizeOf(short[] data) {
        return SIZE_OF_SHORT * data.length;
    }

    public static int sizeOf(int[] data) {
        return SIZE_OF_INTEGER * data.length;
    }

    public static int sizeOf(long[] data) {
        return SIZE_OF_LONG * data.length;
    }

    public static int sizeOf(boolean[] data) {
        return SIZE_OF_FLAG * data.length;
    }

    public static int sizeOf(float[] data) {
        return SIZE_OF_FLOAT * data.length;
    }

    public static int sizeOf(double[] data) {
        return SIZE_OF_DOUBLE * data.length;
    }

    public static int sizeOfByteStrings(byte[][] data) {
        int size = 0;
        for (byte[] bytes : data) {
            size += sizeOfStringLength();
            size += SIZE_OF_BYTE * bytes.length;
        }
        return size;
    }

    public static int sizeOf(UUID[] data) {
        return SIZE_OF_UUID * data.length;
    }

    public static int sizeOfNulls() {
        return 0;
    }

    public static int sizeOf(Vector[] data) {
        int size = 0;
        for (Vector vector : data) {
            size += vector.sizeOf();
        }
        return size;
    }
}
