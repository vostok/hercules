package ru.kontur.vostok.hercules.util.validation;

/**
 * Validator is used to test values with some conditions
 *
 * @author Gregory Koshelev
 */
@FunctionalInterface
public interface Validator<T> {

    /**
     * Validate the value of type {@link T}.
     * <p>
     * Also, {@code null} is acceptable.
     *
     * @param value the value to validate
     * @return validation result
     */
    ValidationResult validate(T value);
}
