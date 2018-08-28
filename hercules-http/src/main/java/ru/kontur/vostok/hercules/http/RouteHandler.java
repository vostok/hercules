package ru.kontur.vostok.hercules.http;

import java.util.Collections;
import java.util.Map;

/**
 * @author Gregory Koshelev
 */
public final class RouteHandler implements AsyncHttpHandler {
    private final Map<String, Map<HttpMethod, AsyncHttpHandler>> handlers;
    private final NotFoundHandler notFoundHandler;

    RouteHandler(Map<String, Map<HttpMethod, AsyncHttpHandler>> handlers) {
        this.handlers = handlers;
        this.notFoundHandler = new NotFoundHandler();
    }

    @Override
    public void handleAsync(HttpServerRequest request) {
        String path = request.getPath();
        HttpMethod method = request.getMethod();
        handlers.getOrDefault(path, Collections.emptyMap()).getOrDefault(method, notFoundHandler).handleAsync(request);
    }
}
