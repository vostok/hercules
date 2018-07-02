package ru.kontur.vostok.hercules.management.api;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.meta.timeline.TimelineRepository;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class HttpServer {
    private final Undertow undertow;

    public HttpServer(Properties properties, AuthManager authManager, StreamRepository streamRepository, TimelineRepository timelineRepository) {
        String host = properties.getProperty("host", "0.0.0.0");
        int port = PropertiesUtil.get(properties, "port", 6307);

        HttpHandler handler = Handlers.routing()
                .get("/ping", exchange -> {
                    exchange.setStatusCode(200);
                    exchange.endExchange();
                });

        undertow = Undertow
                .builder()
                .addHttpListener(port, host)
                .setHandler(handler)
                .build();
    }

    public void start() {
        undertow.start();
    }

    public void stop() {
        undertow.stop();
    }
}
