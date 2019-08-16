package ru.kontur.vostok.hercules.util.parameter;

import org.jetbrains.annotations.NotNull;
import ru.kontur.vostok.hercules.util.validation.ValidationResult;

/**
 * The value of {@link Parameter}
 *
 * @param <T> the value type
 * @author Gregory Koshelev
 */
public class ParameterValue<T> {
    private static final ParameterValue<?> EMPTY = new ParameterValue<>(null, ValidationResult.ok());
    private static final ParameterValue<?> NULL = new ParameterValue<>(null, ValidationResult.notPresent());

    private final T value;
    private final ValidationResult result;

    private ParameterValue(T value, ValidationResult result) {
        this.value = value;
        this.result = result;
    }

    /**
     * Returns valid value. Otherwise throws exception.
     *
     * @return the value of type {@link T}
     * @throws IllegalStateException is value is empty
     * @throws IllegalStateException if value is invalid
     */
    public T get() {
        if (isEmpty()) {
            throw new IllegalStateException("Is empty");
        }

        if (result.isOk()) {
            return value;
        }

        throw new IllegalStateException(result.error());
    }

    /**
     * Returns validation result.
     *
     * @return validation result
     */
    public ValidationResult result() {
        return result;
    }

    /**
     * Returns {@code true} if value is valid.
     *
     * @return {@code true} if value is valid
     */
    public boolean isOk() {
        return result.isOk();
    }

    /**
     * Returns {@code true} if value is invalid.
     *
     * @return {@code true} if value is invalid.
     */
    public boolean isError() {
        return result.isError();
    }

    /**
     * Returns {@code true} if value is empty and it is acceptable.
     *
     * @return {@code true} if value is empty and it is acceptable.
     */
    public boolean isEmpty() {
        return this == EMPTY;
    }

    /**
     * Builds valid parameter's value.
     *
     * @param value the value
     * @param <T>   the value type
     * @return valid parameter's value
     */
    public static <T> ParameterValue<T> of(@NotNull T value) {
        return new ParameterValue<>(value, ValidationResult.ok());
    }

    /**
     * Builds invalid parameter's value.
     *
     * @param result the validation result
     * @param <T>    the value type
     * @return invalid parameter's value
     */
    public static <T> ParameterValue<T> invalid(ValidationResult result) {
        return new ParameterValue<>(null, result);
    }

    /**
     * Builds invalid parameter's value.
     *
     * @param error the validation error
     * @param <T>   the value type
     * @return invalid parameter's value
     */
    public static <T> ParameterValue<T> invalid(String error) {
        return new ParameterValue<>(null, ValidationResult.error(error));
    }

    /**
     * Returns valid empty parameter's value.
     *
     * @param <T> the value type
     * @return valid parameter's value
     */
    @SuppressWarnings("unchecked")
    public static <T> ParameterValue<T> empty() {
        return (ParameterValue<T>) EMPTY;
    }

    /**
     * Returns invalid null paramter's value.
     *
     * @param <T> the value type
     * @return invalid parameter's value
     */
    @SuppressWarnings("unchecked")
    public static <T> ParameterValue<T> ofNull() {
        return (ParameterValue<T>) NULL;
    }
}
