package ru.kontur.vostok.hercules.http;

/**
 * @author Gregory Koshelev
 */
public final class HttpStatusCodes {
    public static final int OK = 200;

    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int METHOD_NOT_ALLOWED = 405;
    public static final int REQUEST_TIMEOUT = 408;
    public static final int CONFLICT = 409;
    /**
     * Request's header 'Content-Length' should be provided
     */
    public static final int LENGTH_REQUIRED = 411;
    /**
     * Request;s header 'Content-Length' value exceeds a limit
     */
    public static final int REQUEST_ENTITY_TOO_LARGE = 413;
    public static final int URI_TOO_LONG = 414;
    public static final int UNSUPPORTED_MEDIA_TIPE= 415;
    public static final int UNPROCESSABLE_ENTITY = 422;
    public static final int TOO_MANY_REQUESTS = 429;

    public static final int INTERNAL_SERVER_ERROR = 500;
    public static final int SERVICE_UNAVAILABLE = 503;

    private HttpStatusCodes() {
    }
}
