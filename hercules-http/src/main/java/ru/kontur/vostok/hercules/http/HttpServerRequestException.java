package ru.kontur.vostok.hercules.http;

/**
 * HTTP server request exception. Optionally may have HTTP status code
 *
 * @author Gregory Koshelev
 */
public class HttpServerRequestException extends Exception {
    private static final int UNDEFINED_STATUS_CODE = 0;

    private final int statusCode;

    public HttpServerRequestException() {
        super();
        statusCode = UNDEFINED_STATUS_CODE;
    }

    public HttpServerRequestException(String message) {
        super(message);
        statusCode = UNDEFINED_STATUS_CODE;
    }

    public HttpServerRequestException(String message, Throwable cause) {
        super(message, cause);
        statusCode = UNDEFINED_STATUS_CODE;
    }

    public HttpServerRequestException(Throwable cause) {
        super(cause);
        statusCode = UNDEFINED_STATUS_CODE;
    }

    public HttpServerRequestException(Throwable cause, int statusCode) {
        super(cause);
        this.statusCode = statusCode;
    }

    /**
     * Get HTTP status code of this exception if this code is not undefined.
     * Otherwise, get specified default HTTP status code
     *
     * @param defaultCode default HTTP status code
     * @return result HTTP status code
     */
    public int getStatusCodeOrDefault(int defaultCode) {
        return (statusCode != UNDEFINED_STATUS_CODE) ? statusCode : defaultCode;
    }
}
