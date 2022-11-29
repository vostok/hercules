package ru.kontur.vostok.hercules.http;

/**
 * Set of predefined HTTP status codes
 *
 * @author Gregory Koshelev
 */
public final class HttpStatusCodes {
    public static final int OK = 200;
    public static final int CREATED = 201;
    public static final int NO_CONTENT = 204;
    public static final int MULTI_STATUS = 207;

    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int METHOD_NOT_ALLOWED = 405;
    public static final int REQUEST_TIMEOUT = 408;
    public static final int CONFLICT = 409;
    /**
     * Resource is unavailable anymore.
     * <p>
     * This code may be used for HTTP readiness probe to indicate that service should not accept new requests.
     */
    public static final int GONE = 410;
    /**
     * Request's header 'Content-Length' should be provided
     */
    public static final int LENGTH_REQUIRED = 411;
    /**
     * Request's header 'Content-Length' value exceeds the limit
     */
    public static final int REQUEST_ENTITY_TOO_LARGE = 413;
    public static final int URI_TOO_LONG = 414;
    public static final int UNSUPPORTED_MEDIA_TYPE = 415;
    public static final int UNPROCESSABLE_ENTITY = 422;
    public static final int TOO_MANY_REQUESTS = 429;
    /**
     * Request cannot be processed properly as client closed it
     */
    public static final int CLIENT_CLOSED_REQUEST = 499;

    public static final int INTERNAL_SERVER_ERROR = 500;
    public static final int NOT_IMPLEMENTED = 501;
    public static final int BAD_GATEWAY = 502;
    public static final int SERVICE_UNAVAILABLE = 503;
    public static final int GATEWAY_TIMEOUT = 504;

    public static final int ORGANIZATION_NOT_FOUND = 1000;
    /**
     * Check if the HTTP status code is {@code 2xx}.
     *
     * @param code the HTTP status code
     * @return {@code true} if the code is {@code 2xx}, otherwise return {@code false}
     */
    public static boolean isSuccess(int code) {
        return 300 > code && code >= 200;
    }

    private HttpStatusCodes() {
        /* static class */
    }
}
