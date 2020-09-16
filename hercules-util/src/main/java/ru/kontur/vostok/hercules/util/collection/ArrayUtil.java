package ru.kontur.vostok.hercules.util.collection;

import java.util.List;

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

    public static float[] toFloatArray(List<?> list) {
        float[] array = new float[list.size()];

        int i = 0;
        for (Object element : list) {
            array[i] = (float) element;
        }

        return array;
    }

    public static boolean[] toBooleanArray(List<?> list) {
        boolean[] array = new boolean[list.size()];

        int i = 0;
        for (Object element : list) {
            array[i] = (boolean) element;
        }

        return array;
    }

    private ArrayUtil() {
        /* static class */
    }
}
