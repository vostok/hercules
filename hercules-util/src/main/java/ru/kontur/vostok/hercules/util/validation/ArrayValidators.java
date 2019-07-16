package ru.kontur.vostok.hercules.util.validation;

/**
 * @author Gregory Koshelev
 */
public final class ArrayValidators {
    public static <T> Validator<T[]> notEmpty() {
        return value ->
                (value.length > 0)
                        ? ValidationResult.ok()
                        : ValidationResult.error("Value should be non empty array");
    }

    private ArrayValidators() {
        /* static class */
    }
}
