package ru.kontur.vostok.hercules.client.exceptions;

/**
 * HerculesClientException
 *
 * @author Kirill Sulim
 */
public class HerculesClientException extends Exception {

    private final Integer httpStatus;

    public HerculesClientException(String message) {
        super(message);
        this.httpStatus = null;
    }

    public HerculesClientException(String message, int httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public HerculesClientException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = null;
    }

    public HerculesClientException(String message, int httpStatus, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }
}
