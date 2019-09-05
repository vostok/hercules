package ru.kontur.vostok.hercules.util.parameter.parsing;

import ru.kontur.vostok.hercules.util.parameter.ParameterValue;

/**
 * Parse string to value of type {@link T}
 *
 * @author Gregory Koshelev
 */
@FunctionalInterface
public interface Parser<T> {
    /**
     * Parse string to value of type {@link T}. If string is null thus returns empty value.
     *
     * @param value the string to be parsed
     * @return parsed value
     */
    ParameterValue<T> parse(String value);
}
