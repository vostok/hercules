package ru.kontur.vostok.hercules.http.handler;

import ru.kontur.vostok.hercules.http.HttpMethod;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.http.NotSupportedHttpMethodException;

import java.util.Collections;
import java.util.Map;

/**
 * @author Gregory Koshelev
 */
public final class RouteHandler implements AsyncHttpHandler {
    private final Map<String, Map<HttpMethod, AsyncHttpHandler>> handlers;

    RouteHandler(Map<String, Map<HttpMethod, AsyncHttpHandler>> handlers) {
        this.handlers = handlers;
    }

    @Override
    public void handleAsync(HttpServerRequest request) {
        String path = request.getPath();
        HttpMethod method;
        try {
            method = request.getMethod();
        } catch (NotSupportedHttpMethodException e) {
            request.complete(HttpStatusCodes.METHOD_NOT_ALLOWED);
            return;
        }

        Map<HttpMethod, AsyncHttpHandler> map = handlers.getOrDefault(path, Collections.emptyMap());
        if (map.isEmpty()) {
            request.complete(HttpStatusCodes.NOT_FOUND);
            return;
        }
        AsyncHttpHandler handler = map.get(method);
        if (handler == null) {
            request.complete(HttpStatusCodes.METHOD_NOT_ALLOWED);
            return;
        }

        handler.handleAsync(request);
    }
}
