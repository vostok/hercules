package ru.kontur.vostok.hercules.util.bytes;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * @author Gregory Koshelev
 */
public class ByteUtil {
    private static final char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();

    private static final byte[] HEX_CHAR_TO_BYTE;

    static {
        byte[] hexCharToByte = new byte[256];
        Arrays.fill(hexCharToByte, (byte) -1);
        for (char hex = '0'; hex <= '9'; hex++) {
            hexCharToByte[hex] = (byte) (hex - '0');
        }
        for (char hex = 'a'; hex <= 'f'; hex++) {
            hexCharToByte[hex] = (byte) (hex - 'a' + 0x0A);
        }
        for (char hex = 'A'; hex <= 'F'; hex++) {
            hexCharToByte[hex] = (byte) (hex - 'A' + 0x0A);
        }

        HEX_CHAR_TO_BYTE = hexCharToByte;
    }

    /**
     * Check if array starts with subarray
     *
     * @param array
     * @param subarray
     * @return true if array starts with subarray
     */
    public static boolean isSubarray(byte[] array, byte[] subarray) {
        if (array.length < subarray.length) {
            return false;
        }
        for (int i = 0; i < subarray.length; i++) {
            if (subarray[i] != array[i]) {
                return false;
            }
        }
        return true;
    }

    public static long toLong(byte[] bytes) {
        if (bytes == null || bytes.length != 8) {
            throw new IllegalArgumentException("The length of byte array should be equal to 8");
        }
        return ((bytes[0] & 0xFFL) << 56)
                | ((bytes[1] & 0xFFL) << 48)
                | ((bytes[2] & 0xFFL) << 40)
                | ((bytes[3] & 0xFFL) << 32)
                | ((bytes[4] & 0xFFL) << 24)
                | ((bytes[5] & 0xFFL) << 16)
                | ((bytes[6] & 0xFFL) << 8)
                | (bytes[7] & 0xFFL);
    }

    public static byte[] fromByteBuffer(ByteBuffer buffer) {
        int size = buffer.remaining();
        byte[] bytes = new byte[size];
        buffer.get(bytes);
        return bytes;
    }

    public static String toHexString(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2 + 2];
        hexChars[0] = '0';
        hexChars[1] = 'x';

        return toRawHexString(bytes, hexChars, 2);
    }

    private static String toRawHexString(byte[] bytes, char[] hexChars, int offset) {
        for (int i = 0; i < bytes.length; i++) {
            toHexChars(bytes[i], hexChars, offset + i * 2);
        }
        return new String(hexChars);
    }

    public static String toHexString(ByteBuffer buffer) {
        char[] hexChars = new char[buffer.remaining() * 2 + 2];
        hexChars[0] = '0';
        hexChars[1] = 'x';

        return toRawHexString(buffer, hexChars, 2);
    }

    private static String toRawHexString(ByteBuffer buffer, char[] hexChars, int offset) {
        int size = buffer.remaining();

        for (int i = 0; i < size; i++) {
            toHexChars(buffer.get(), hexChars, offset + i * 2);
        }
        return new String(hexChars);
    }

    public static byte[] fromHexString(@NotNull String string) {
        if (string.length() < 2 ||
                string.length() % 2 != 0 ||
                string.charAt(0) != '0' ||
                string.charAt(1) != 'x') {
            throw new IllegalArgumentException("Not a hex string: " + string);
        }

        return fromRawHexString(string, 2);
    }

    public static int overallLength(byte[][] bytes) {
        int size = 0;
        for (byte[] bb : bytes) {
            size += bb.length;
        }
        return size;
    }

    /**
     * Returns the index within source array of the first occurrence of the {@code needle} byte
     * starting from the {@code offset} position.
     * <p>
     * If no {@code needle} byte occurs, then {@code -1} is returned.
     *
     * @param source the byte array
     * @param needle the byte to search
     * @param offset the starting position
     * @return index of the first occurrence of the {@code needle} byte or {@code -1} if no {@code needle} byte occurs
     */
    public static int find(byte[] source, byte needle, int offset) {
        for (int i = offset; i < source.length; i++) {
            if (source[i] == needle) {
                return i;
            }
        }
        return -1;
    }

    private static byte[] fromRawHexString(String string, int offset) {
        byte[] bytes = new byte[(string.length() - offset) / 2];

        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = fromHexChars(string.charAt(i * 2 + offset), string.charAt(i * 2 + offset + 1));
        }

        return bytes;
    }

    private static void toHexChars(byte b, char[] hexChars, int offset) {
        hexChars[offset] = HEX_CHARS[(b & 0xF0) >> 4];
        hexChars[offset + 1] = HEX_CHARS[b & 0x0F];
    }

    private static byte fromHexChars(char left, char right) {
        byte leftByteHalf = HEX_CHAR_TO_BYTE[left];
        byte rightByteHalf = HEX_CHAR_TO_BYTE[right];
        if (leftByteHalf == -1 || rightByteHalf == -1) {
            throw new IllegalArgumentException("Not a hex chars: " + left + right);
        }
        return (byte) (leftByteHalf << 4 | rightByteHalf);
    }
}
