package ru.kontur.vostok.hercules.util.validation;

import java.util.Optional;

/**
 * @author Gregory Koshelev
 */
public final class LongValidators {
    public static Validator<Long> positive() {
        return value -> {
            if (value > 0) {
                return Optional.empty();
            }
            return Optional.of("Value should be positive long but was " + value);
        };
    }

    private LongValidators() {
        /* static class */
    }
}
