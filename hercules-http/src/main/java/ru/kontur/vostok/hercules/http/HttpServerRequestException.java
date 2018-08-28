package ru.kontur.vostok.hercules.http;

/**
 * @author Gregory Koshelev
 */
public class HttpServerRequestException extends Exception {
    public HttpServerRequestException() {
        super();
    }

    public HttpServerRequestException(String message) {
        super(message);
    }

    public HttpServerRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public HttpServerRequestException(Throwable cause) {
        super(cause);
    }
}
