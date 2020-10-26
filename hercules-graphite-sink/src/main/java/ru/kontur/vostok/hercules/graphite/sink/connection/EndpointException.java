package ru.kontur.vostok.hercules.graphite.sink.connection;

/**
 * @author Gregory Koshelev
 */
public class EndpointException extends Exception {
    public EndpointException(String message) {
        super(message);
    }

    public EndpointException(String message, Throwable cause) {
        super(message, cause);
    }
}
