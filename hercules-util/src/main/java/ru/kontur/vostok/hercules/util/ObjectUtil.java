package ru.kontur.vostok.hercules.util;

import java.util.Objects;

/**
 * ObjectUtil - collection of object util functions
 *
 * @author Kirill Sulim
 */
public final class ObjectUtil {

    public static <T> T firstNonNull(T ... values) {
        for (T value : values) {
            if (Objects.nonNull(value)) {
                return value;
            }
        }
        return null;
    }

    private ObjectUtil() {
        /* static class */
    }
}
