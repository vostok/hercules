package ru.kontur.hercules.tracing.api;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.undertow.util.handlers.HerculesRoutingHandler;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;
import ru.kontur.vostok.hercules.util.validation.IntegerValidators;

import java.util.Properties;

/**
 * HttpServer
 *
 * @author Kirill Sulim
 */
public class HttpServer {

    private static class Props {
        static final PropertyDescription<String> HOST = PropertyDescriptions
            .stringProperty("host")
            .withDefaultValue("0.0.0.0")
            .build();

        static final PropertyDescription<Integer> PORT = PropertyDescriptions
            .integerProperty("port")
            .withDefaultValue(6310)
            .withValidator(IntegerValidators.portValidator())
            .build();
    }

    private final Undertow undertow;

    public HttpServer(
        Properties properties,
        GetTraceHandler getTraceHandler,
        MetricsCollector metricsCollector
    ) {
        final String host = Props.HOST.extract(properties);
        final int port = Props.PORT.extract(properties);

        HttpHandler handler = new HerculesRoutingHandler(metricsCollector)
            .get("/trace", getTraceHandler);

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
