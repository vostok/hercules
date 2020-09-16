package ru.kontur.vostok.hercules.protocol;

/**
 * Define size in bytes for data types and auxiliary structures.
 *
 * @author Gregory Koshelev
 */
public class Sizes {
    private static final int SIZE_OF_STRING_LENGTH = Type.INTEGER.size;

    /**
     * Size of string length.
     *
     * @return size of string length
     */
    private static int sizeOfStringLength() {
        return SIZE_OF_STRING_LENGTH;
    }

    public static int sizeOfString(byte[] data) {
        return sizeOfStringLength() + Type.BYTE.size * data.length;
    }
}
