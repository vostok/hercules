package ru.kontur.vostok.hercules.util.validation;

/**
 * @author Gregory Koshelev
 */
public final class IntegerValidators {
    public static Validator<Integer> positive() {
        return value ->
                (value != null && value > 0)
                        ? ValidationResult.ok()
                        : ValidationResult.error("Value should be positive integer but was " + value);
    }

    public static Validator<Integer> nonNegative() {
        return value ->
                (value != null && value >= 0)
                        ? ValidationResult.ok()
                        : ValidationResult.error("Value should be non negative integer but was " + value);
    }

    /**
     * Range validator checks if value is in range from {@code left} value to {@code right} value.
     * The {@code right} value is exclusive bound.
     *
     * @param left  left bound, inclusive
     * @param right right bound, exclusive
     * @return Range validator
     */
    public static Validator<Integer> range(int left, int right) {
        return value ->
                (value != null && value >= left && value < right)
                        ? ValidationResult.ok()
                        : ValidationResult.error("Value should be in range [" + left + ", " + right + ")");
    }

    /**
     * Range validator checks if value is in range from {@code left} value to {@code right} value inclusive.
     *
     * @param left  left bound, inclusive
     * @param right right bound, inclusive
     * @return Range validator
     */
    public static Validator<Integer> rangeInclusive(int left, int right) {
        return value ->
                (value != null && value >= left && value <= right)
                        ? ValidationResult.ok()
                        : ValidationResult.error("Value should be in range [" + left + ", " + right + "]");
    }

    /**
     * Port validator
     *
     * @return validator
     */
    public static Validator<Integer> portValidator() {
        return value ->
                (value != null && value >= 0 && value <= 65_535)
                        ? ValidationResult.ok()
                        : ValidationResult.error("Valid port number is between 0 and 65 535");
    }

    public IntegerValidators() {
        /* static class */
    }
}
