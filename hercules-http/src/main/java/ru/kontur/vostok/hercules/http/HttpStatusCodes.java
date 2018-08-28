package ru.kontur.vostok.hercules.http;

/**
 * @author Gregory Koshelev
 */
public class HttpStatusCodes {
    public static final int OK = 200;

    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int CONFLICT = 409;
    public static final int UNPROCESSABLE_ENTITY = 422;

    public static final int INTERNAL_SERVER_ERROR = 500;
    public static final int SERVICE_UNAVAILABLE = 503;

    private HttpStatusCodes() {
    }
}
