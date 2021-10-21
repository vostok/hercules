package ru.kontur.vostok.hercules.tracing.api;

import ru.kontur.vostok.hercules.application.Application;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.health.CommonMetrics;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.http.HttpServer;
import ru.kontur.vostok.hercules.http.handler.RouteHandler;
import ru.kontur.vostok.hercules.json.format.EventToJsonFormatter;
import ru.kontur.vostok.hercules.undertow.util.UndertowHttpServer;
import ru.kontur.vostok.hercules.http.handler.InstrumentedRouteHandlerBuilder;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Properties;

/**
 * Tracing API Application
 *
 * @author Gregory Koshelev
 */
public class TracingApiApplication {
    private static MetricsCollector metricsCollector;
    private static TracingReader tracingReader;
    private static EventToJsonFormatter eventFormatter;

    public static void main(String[] args) {
        Application.run("Hercules Tracing API", "tracing-api", args, (properties, container) -> {
            Properties metricsProperties = PropertiesUtil.ofScope(properties, Scopes.METRICS);
            Properties httpServerProperties = PropertiesUtil.ofScope(properties, Scopes.HTTP_SERVER);
            Properties readerProperties = PropertiesUtil.ofScope(properties, "reader");
            Properties formatterProperties = PropertiesUtil.ofScope(properties, "tracing.format");

            metricsCollector = container.register(new MetricsCollector(metricsProperties));
            CommonMetrics.registerCommonMetrics(metricsCollector);

            tracingReader = container.register(TracingReader.createTracingReader(readerProperties));

            eventFormatter = new EventToJsonFormatter(formatterProperties);

            container.register(createHttpServer(httpServerProperties));
        });
    }

    private static HttpServer createHttpServer(Properties httpServerProperties) {
        RouteHandler handler = new InstrumentedRouteHandlerBuilder(httpServerProperties, metricsCollector).
                get("/trace", new GetTraceHandler(tracingReader, eventFormatter)).
                build();

        return new UndertowHttpServer(
                Application.application().getConfig().getHost(),
                Application.application().getConfig().getPort(),
                httpServerProperties,
                handler);
    }
}
