package ru.kontur.vostok.hercules.http;

/**
 * Supported HTTP methods list
 *
 * @author Gregory Koshelev
 */
public enum HttpMethod {
    GET,
    POST,
    PUT,
    DELETE;

    public static HttpMethod parse(String value) throws NotSupportedHttpMethodException {
        try {
            return HttpMethod.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new NotSupportedHttpMethodException("Unsupported method " + value);
        }
    }
}
