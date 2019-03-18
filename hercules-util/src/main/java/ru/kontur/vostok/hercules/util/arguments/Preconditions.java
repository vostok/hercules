package ru.kontur.vostok.hercules.util.arguments;

import java.util.Objects;

/**
 * Preconditions
 *
 * @author Kirill Sulim
 */
public final class Preconditions {

    public static void check(boolean condition) {
        if (!condition) {
            throw new IllegalArgumentException();
        }
    }

    public static void check(boolean condition, String errorMessage) {
        if (!condition) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    public static void checkNotNull(Object o) {
        check(Objects.nonNull(o));
    }

    public static void checkNotNull(Object o, String errorMessage) {
        check(Objects.nonNull(o), errorMessage);
    }
}
