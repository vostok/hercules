package ru.kontur.vostok.hercules.util.collection;

/**
 * @author Gregory Koshelev
 */
public final class ArrayUtil {
    public static <T> boolean contains(T[] array, T obj) {
        if (array.length == 0) {
            return false;
        }

        for (T element : array) {
            if (element.equals(obj)) {
                return true;
            }
        }
        return false;
    }

    private ArrayUtil() {
        /* static class */
    }
}
