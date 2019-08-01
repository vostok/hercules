package ru.kontur.vostok.hercules.util.parameter;

/**
 * The type of {@link Parameter}
 *
 * @author Gregory Koshelev
 */
public enum ParameterType {
    /**
     * Required parameter.
     */
    REQUIRED,
    /**
     * Optional parameter with default value. Default value is used if no other value is specified
     */
    DEFAULT,
    /**
     * Optional parameter without default value.
     */
    EMPTY;
}
