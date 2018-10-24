package ru.kontur.vostok.hercules.stream.api;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.undertow.util.PingHandler;
import ru.kontur.vostok.hercules.undertow.util.metrics.MetricsHandler;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;
import ru.kontur.vostok.hercules.util.validation.Validators;

import java.util.Properties;

public class HttpServer {

    private static class Props {
        static final PropertyDescription<String> HOST = PropertyDescriptions
                .stringProperty("host")
                .withDefaultValue("0.0.0.0")
                .build();

        static final PropertyDescription<Integer> PORT = PropertyDescriptions
                .integerProperty("port")
                .withDefaultValue(6307)
                .withValidator(Validators.portValidator())
                .build();
    }

    private final Undertow undertow;

    public HttpServer(
            Properties properties,
            AuthManager authManager,
            ReadStreamHandler readStreamHandler,
            MetricsCollector metricsCollector
    ) {
        final String host = Props.HOST.extract(properties);
        final int port = Props.PORT.extract(properties);

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
