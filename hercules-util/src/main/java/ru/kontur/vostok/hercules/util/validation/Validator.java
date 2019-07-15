package ru.kontur.vostok.hercules.util.validation;

import java.util.Optional;

/**
 * Validator is used to test values with some conditions
 *
 * @author Gregory Koshelev
 */
@FunctionalInterface
public interface Validator<T> {

    /**
     * Validate the value of type {@link T}
     *
     * @param value the value to validate
     * @return validation result
     */
    ValidationResult validate(T value);
}
