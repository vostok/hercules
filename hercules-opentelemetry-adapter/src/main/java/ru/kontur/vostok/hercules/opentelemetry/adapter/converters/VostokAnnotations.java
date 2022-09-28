package ru.kontur.vostok.hercules.opentelemetry.adapter.converters;

import ru.kontur.vostok.hercules.protocol.TinyString;

/**
 * Vostok tracing annotations
 *
 * @author Innokentiy Krivonosov
 * @see <a href="https://github.com/vostok/tracing.abstractions/blob/master/README.md">Vostok tracing</a>
 */
public class VostokAnnotations {

    /**
     * Common annotations
     */
    protected static final TinyString KIND = TinyString.of("kind");
    protected static final TinyString OPERATION = TinyString.of("operation");
    protected static final TinyString STATUS = TinyString.of("status");
    protected static final TinyString APPLICATION = TinyString.of("application");
    protected static final TinyString HOST = TinyString.of("host");

    /**
     * HTTP requests
     */
    protected static final TinyString HTTP_REQUEST_METHOD = TinyString.of("http.request.method");
    protected static final TinyString HTTP_REQUEST_URL = TinyString.of("http.request.url");
    protected static final TinyString HTTP_REQUEST_SIZE = TinyString.of("http.request.size");
    protected static final TinyString HTTP_RESPONSE_CODE = TinyString.of("http.response.code");
    protected static final TinyString HTTP_RESPONSE_SIZE = TinyString.of("http.response.size");

    /**
     * HTTP server
     */
    protected static final TinyString HTTP_CLIENT_NAME = TinyString.of("http.client.name");
    protected static final TinyString HTTP_CLIENT_ADDRESS = TinyString.of("http.client.address");
}
