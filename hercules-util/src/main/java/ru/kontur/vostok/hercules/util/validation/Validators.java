package ru.kontur.vostok.hercules.util.validation;

/**
 * Validators
 *
 * @author Gregory Koshelev
 */
public final class Validators {
    private static Validator<?> ANY = (x) -> ValidationResult.ok();
    private static Validator<?> NOT_NULL = (x) -> x != null ? ValidationResult.ok() : ValidationResult.missed();

    /**
     * Validator accepts any value. Also, null value is acceptable.
     *
     * @param <T> the value type
     * @return validator
     */
    @SuppressWarnings("unchecked")
    public static <T> Validator<T> any() {
        return (Validator<T>) ANY;
    }

    /**
     * Validator accepts not null values.
     *
     * @param <T> the value type
     * @return validator
     */
    @SuppressWarnings("unchecked")
    public static <T> Validator<T> notNull() {
        return (Validator<T>) NOT_NULL;
    }

    /**
     * Validator accepts values which are accepted by both validators.
     *
     * @param v1  the first validator
     * @param v2  the second validator
     * @param <T> the value type
     * @return validator
     */
    public static <T> Validator<T> and(Validator<T> v1, Validator<T> v2) {
        return (x) -> {
            ValidationResult result = v1.validate(x);
            return result.isOk() ? v2.validate(x) : result;
        };
    }

    /**
     * Validator accepts values which are accepted by at least one validator.
     *
     * @param v1  the first validator
     * @param v2  the second validator
     * @param <T> the value type
     * @return validator
     */
    public static <T> Validator<T> or(Validator<T> v1, Validator<T> v2) {
        return (x) -> {
            ValidationResult result = v1.validate(x);
            return result.isOk() ? result : v2.validate(x);
        };
    }

    private Validators() {
        /* static class */
    }
}
