package ru.kontur.vostok.hercules.util.validation;

/**
 * @author Gregory Koshelev
 */
public final class LongValidators {
    public static Validator<Long> positive() {
        return value ->
                (value > 0)
                        ? ValidationResult.ok()
                        : ValidationResult.error("Value should be positive long but was " + value);
    }

    public static Validator<Long> nonNegative() {
        return value ->
                (value >= 0)
                        ? ValidationResult.ok()
                        : ValidationResult.error("Value should be non negative long but was " + value);
    }

    private LongValidators() {
        /* static class */
    }
}
