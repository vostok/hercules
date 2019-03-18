package ru.kontur.vostok.hercules.meta.serialization;

/**
 * @author Gregory Koshelev
 */
public class DeserializationException extends Exception {
    public DeserializationException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
