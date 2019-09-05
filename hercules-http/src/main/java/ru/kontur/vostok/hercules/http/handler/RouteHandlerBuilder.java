package ru.kontur.vostok.hercules.http.handler;

import ru.kontur.vostok.hercules.http.HttpMethod;
import ru.kontur.vostok.hercules.http.HttpServer;
import ru.kontur.vostok.hercules.http.UrlUtil;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Builder for RouteHandler. Builder doesn't thread safe.
 *
 * @author Gregory Koshelev
 */
public class RouteHandlerBuilder {
    private final Map<String, Map<HttpMethod, HttpHandler>> handlers = new HashMap<>();
    private final String rootPath;

    public RouteHandlerBuilder(Properties properties) {
        this.rootPath = PropertiesUtil.get(HttpServer.Props.ROOT_PATH, properties).get();
    }

    public void addHandler(String path, HttpMethod method, HttpHandler handler) {
        Map<HttpMethod, HttpHandler> map = handlers.computeIfAbsent(UrlUtil.join(rootPath, path), s -> new HashMap<>());
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
