package ru.kontur.vostok.hercules.undertow.util.servers;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.RoutingHandler;
import ru.kontur.vostok.hercules.undertow.util.handlers.AboutHandler;
import ru.kontur.vostok.hercules.undertow.util.handlers.PingHandler;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;
import ru.kontur.vostok.hercules.util.validation.Validators;

import java.util.Properties;

/**
 * MinimalStatusServer
 *
 * @author Kirill Sulim
 */
public class MinimalStatusServer {

    private static class Props {
        static final PropertyDescription<String> HOST = PropertyDescriptions
                .stringProperty("host")
                .withDefaultValue("0.0.0.0")
                .build();

        static final PropertyDescription<Integer> PORT = PropertyDescriptions
                .integerProperty("port")
                .withValidator(Validators.portValidator())
                .build();
    }

    private final Undertow undertow;

    public MinimalStatusServer(Properties statusServerProperties) {

        final String host = Props.HOST.extract(statusServerProperties);
        final int port = Props.PORT.extract(statusServerProperties);

        RoutingHandler handler = Handlers.routing()
                .get("/ping", PingHandler.INSTANCE)
                .get("/about", AboutHandler.INSTANCE);

        undertow = Undertow.builder()
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
