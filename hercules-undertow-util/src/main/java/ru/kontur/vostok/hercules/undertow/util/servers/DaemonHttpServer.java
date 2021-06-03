package ru.kontur.vostok.hercules.undertow.util.servers;

import ru.kontur.vostok.hercules.application.Application;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.http.HttpServer;
import ru.kontur.vostok.hercules.http.handler.RouteHandler;
import ru.kontur.vostok.hercules.undertow.util.UndertowHttpServer;
import ru.kontur.vostok.hercules.undertow.util.handlers.InstrumentedRouteHandlerBuilder;
import ru.kontur.vostok.hercules.util.lifecycle.Lifecycle;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class DaemonHttpServer implements Lifecycle {
    private final UndertowHttpServer httpServer;

    public DaemonHttpServer(Properties properties, MetricsCollector metricsCollector) {
        properties = override(properties);

        RouteHandler handler =
                new InstrumentedRouteHandlerBuilder(properties, metricsCollector).build();

        httpServer = new UndertowHttpServer(
                Application.application().getConfig().getHost(),
                Application.application().getConfig().getPort(),
                properties,
                handler);
    }

    @Override
    public void start() {
        httpServer.start();
    }

    @Override
    public boolean stop(long timeout, TimeUnit unit) {
        return httpServer.stop(timeout, unit);
    }

    protected Properties override(Properties properties) {
        Properties overriddenProperties = PropertiesUtil.copy(properties);
        overriddenProperties.putIfAbsent(HttpServer.Props.IO_THREADS, "1");
        overriddenProperties.putIfAbsent(HttpServer.Props.WORKER_THREADS, "1");
        return overriddenProperties;
    }
}
