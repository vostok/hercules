package ru.kontur.vostok.hercules.util.arrays;

public class ArrayUtil {

    public static final int NOT_FOUND_INDEX = -1;

    public static <T> int indexOf(T[] array, T element) {
        for (int i = 0; i < array.length; ++i) {
            if (array[i] == element) {
                return i;
            }
        }
        return NOT_FOUND_INDEX;
    }
}
