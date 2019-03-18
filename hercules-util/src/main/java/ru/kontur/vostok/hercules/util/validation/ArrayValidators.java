package ru.kontur.vostok.hercules.util.validation;

import java.util.Optional;

/**
 * @author Gregory Koshelev
 */
public final class ArrayValidators {
    public static <T> Validator<T[]> notEmpty() {
        return value -> {
            if (value.length > 0) {
                return Optional.empty();
            }
            return Optional.of("Value should be non empty array");
        };
    }

    private ArrayValidators() {
        /* static class */
    }
}
