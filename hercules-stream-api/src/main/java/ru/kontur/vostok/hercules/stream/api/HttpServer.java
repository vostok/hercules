package ru.kontur.vostok.hercules.stream.api;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.undertow.util.PingHandler;
import ru.kontur.vostok.hercules.undertow.util.metrics.MetricsHandler;
import ru.kontur.vostok.hercules.util.properties.PropertiesExtractor;

import java.util.Properties;

public class HttpServer {

    private final Undertow undertow;

    public HttpServer(
            Properties properties,
            AuthManager authManager,
            ReadStreamHandler readStreamHandler,
            MetricsCollector metricsCollector
    ) {
        String host = properties.getProperty("host", "0.0.0.0");
        int port = PropertiesExtractor.get(properties, "port", 6307);




        HttpHandler handler = Handlers.routing()
                .get(
                        "/ping",
                        new MetricsHandler(PingHandler.INSTANCE, "ping", metricsCollector)
                )
                .post(
                        "/stream/read",
                        new MetricsHandler(readStreamHandler, "stream_read", metricsCollector)
                );

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
