package ru.kontur.vostok.hercules.util.parameter.parsing;

import org.jetbrains.annotations.NotNull;

/**
 * The parsing result:
 * <ul>
 *     <li>is empty if {@link Parser} treats the input as an empty value,</li>
 *     <li>contains the parsed value,</li>
 *     <li>or contains the error information in case of an any error.</li>
 * </ul>
 *
 * @author Gregory Koshelev
 */
public class ParsingResult<T> {
    private static final ParsingResult<?> EMPTY = new ParsingResult<>(null, null);

    private final T value;
    private final String error;

    private ParsingResult(T value, String error) {
        this.value = value;
        this.error = error;
    }

    /**
     * Return {@code true} if the parsing result is empty.
     *
     * @return {@code true} if the parsing result is empty
     */
    public boolean isEmpty() {
        return this == EMPTY;
    }

    /**
     * Return {@code true} if the parsing result contains the value.
     *
     * @return {@code true} if the parsing result contains the value
     */
    public boolean hasValue() {
        return value != null;
    }

    /**
     * Return {@code true} if the parsing result contains the error.
     *
     * @return {@code true) if the parsing result contains the error}
     */
    public boolean hasError() {
        return error != null;
    }

    /**
     * Return the parsed value if it exists or throw {@link IllegalStateException} if it doesn't¬.
     *
     * @return the parsed value
     * @throws IllegalStateException if the result is empty or has the error
     */
    @NotNull
    public T get() {
        if (isEmpty()) {
            throw new IllegalStateException("Is empty");
        }

        if (hasValue()) {
            return value;
        }

        throw new IllegalStateException(error);
    }

    /**
     * Return the error if it exists or {@code null} otherwise.
     *
     * @return the error if it exists or {@code null} otherwise
     */
    public String error() {
        return error;
    }

    /**
     * Return the parsed value if it exists or default value otherwise.
     * @param defaultValue the default value
     * @return parsed value if it exists or defaultValue otherwise.
     */
    public T orElse(T defaultValue) {
        return hasValue() ? value : defaultValue;
    }
    
    /**
     * The empty result.
     *
     * @param <T> the value type
     * @return the empty result
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <T> ParsingResult<T> empty() {
        return (ParsingResult<T>) EMPTY;
    }

    /**
     * The valid result with the parsed value.
     *
     * @param value the parsed value
     * @param <T>   the value type
     * @return the valid result
     */
    @NotNull
    public static <T> ParsingResult<T> of(@NotNull T value) {
        return new ParsingResult<>(value, null);
    }

    /**
     * The invalid result with the parsing error.
     *
     * @param error the parsing error
     * @param <T>   the value type
     * @return the invalid result
     */
    @NotNull
    public static <T> ParsingResult<T> invalid(@NotNull String error) {
        return new ParsingResult<>(null, error);
    }
}
