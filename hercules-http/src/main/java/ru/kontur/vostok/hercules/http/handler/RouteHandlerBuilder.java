package ru.kontur.vostok.hercules.http.handler;

import ru.kontur.vostok.hercules.http.HttpMethod;

import java.util.HashMap;
import java.util.Map;

/**
 * Builder for RouteHandler. Builder doesn't thread safe.
 *
 * @author Gregory Koshelev
 */
public class RouteHandlerBuilder {
    private final Map<String, Map<HttpMethod, HttpHandler>> handlers = new HashMap<>();

    public void addHandler(String path, HttpMethod method, HttpHandler handler) {
        Map<HttpMethod, HttpHandler> map = handlers.computeIfAbsent(path, s -> new HashMap<>());
        map.putIfAbsent(method, handler);
    }

    public RouteHandlerBuilder get(String path, HttpHandler handler) {
        addHandler(path, HttpMethod.GET, handler);
        return this;
    }

    public RouteHandlerBuilder post(String path, HttpHandler handler) {
        addHandler(path, HttpMethod.POST, handler);
        return this;
    }

    public RouteHandlerBuilder put(String path, HttpHandler handler) {
        addHandler(path, HttpMethod.PUT, handler);
        return this;
    }

    public RouteHandlerBuilder delete(String path, HttpHandler handler) {
        addHandler(path, HttpMethod.DELETE, handler);
        return this;
    }

    public RouteHandler build() {
        return new RouteHandler(handlers);
    }
}
