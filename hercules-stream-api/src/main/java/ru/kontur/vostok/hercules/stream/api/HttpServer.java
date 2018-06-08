package ru.kontur.vostok.hercules.stream.api;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Properties;

public class HttpServer {

    private final Undertow undertow;

    public HttpServer(Properties properties, AuthManager authManager, ReadStreamHandler readStreamHandler) {
        String host = properties.getProperty("host", "0.0.0.0");
        int port = PropertiesUtil.get(properties, "port", PropertiesUtil.get(properties, "server.port", 80));

        HttpHandler handler = Handlers.routing()
                .get("/ping", exchange -> {
                    exchange.setStatusCode(200);
                    exchange.endExchange();
                })
                .post("/stream/read", readStreamHandler);

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
