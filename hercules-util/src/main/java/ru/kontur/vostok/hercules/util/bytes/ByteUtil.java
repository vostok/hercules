package ru.kontur.vostok.hercules.util.bytes;

/**
 * @author Gregory Koshelev
 */
public class ByteUtil {
    /**
     * Check if array starts with subarray
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
}
