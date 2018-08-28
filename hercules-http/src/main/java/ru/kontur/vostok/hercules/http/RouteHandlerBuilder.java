package ru.kontur.vostok.hercules.http;

import java.util.HashMap;
import java.util.Map;

/**
 * Builder for RouteHandler. Builder doesn't thread safe.
 *
 * @author Gregory Koshelev
 */
public class RouteHandlerBuilder {
    private final Map<String, Map<HttpMethod, AsyncHttpHandler>> handlers = new HashMap<>();

    public void addHandler(String path, HttpMethod method, AsyncHttpHandler handler) {
        Map<HttpMethod, AsyncHttpHandler> map = handlers.computeIfAbsent(path, s -> new HashMap<>());
        map.putIfAbsent(method, handler);
    }

    public RouteHandler build() {
        return new RouteHandler(handlers);
    }
}
