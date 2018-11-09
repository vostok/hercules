package ru.kontur.vostok.hercules.util.validation;

import java.util.Optional;

/**
 * @author Gregory Koshelev
 */
public final class IntegerValidators {
    public static Validator<Integer> positive() {
        return value -> {
            if (value > 0) {
                return Optional.empty();
            }
            return Optional.of("Value should be positive integer but was " + value);
        };
    }

    public IntegerValidators() {
        /* static class */
    }
}
