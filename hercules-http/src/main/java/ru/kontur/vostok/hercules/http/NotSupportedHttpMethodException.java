package ru.kontur.vostok.hercules.http;

/**
 * @author Gregory Koshelev
 */
public class NotSupportedHttpMethodException extends Exception {
    public NotSupportedHttpMethodException(String message) {
        super(message);
    }
}
