package ru.kontur.vostok.hercules.undertow.util.servers;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.RoutingHandler;
import ru.kontur.vostok.hercules.undertow.util.handlers.AboutHandler;
import ru.kontur.vostok.hercules.undertow.util.handlers.PingHandler;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.validation.IntegerValidators;

import java.util.Properties;

/**
 * ApplicationStatusHttpServer - minimal HTTP server with base information about application
 *
 * @author Kirill Sulim
 * @deprecated use {@link DaemonHttpServer} instead (be careful: {@link ru.kontur.vostok.hercules.application.Application} should be used too
 */
@Deprecated
public class ApplicationStatusHttpServer {

    private final Undertow undertow;

    public ApplicationStatusHttpServer(Properties statusServerProperties) {

        final String host = PropertiesUtil.get(Props.HOST, statusServerProperties).get();
        final int port = PropertiesUtil.get(Props.PORT, statusServerProperties).get();

        RoutingHandler handler = Handlers.routing()
                .get("/ping", PingHandler.INSTANCE)
                .get("/about", AboutHandler.INSTANCE);

        undertow = Undertow.builder()
                .addHttpListener(port, host)
                .setHandler(handler)
                .setIoThreads(2)
                .setWorkerThreads(1)
                .build();
    }

    public void start() {
        undertow.start();
    }

    public void stop() {
        undertow.stop();
    }

    private static class Props {
        static final Parameter<String> HOST =
                Parameter.stringParameter("host").
                        withDefault("0.0.0.0").
                        build();

        static final Parameter<Integer> PORT =
                Parameter.integerParameter("port").
                        withValidator(IntegerValidators.portValidator()).
                        required().
                        build();
    }
}
