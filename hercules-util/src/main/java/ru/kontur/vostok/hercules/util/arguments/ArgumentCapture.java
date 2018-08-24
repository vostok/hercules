package ru.kontur.vostok.hercules.util.arguments;

import java.util.Objects;

/**
 * ArgumentCapture
 *
 * @author Kirill Sulim
 */
public class ArgumentCapture<T> {

    protected final T argument;

    public ArgumentCapture(T argument) {
        this.argument = argument;
    }

    public void isNotNull() {
        if (Objects.isNull(argument)) {
            throw new IllegalArgumentException("Argument cannot be null");
        }
    }
}
