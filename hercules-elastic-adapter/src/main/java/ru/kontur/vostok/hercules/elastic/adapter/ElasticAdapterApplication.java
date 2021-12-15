package ru.kontur.vostok.hercules.elastic.adapter;

import ru.kontur.vostok.hercules.application.Application;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.elastic.adapter.gate.GateSender;
import ru.kontur.vostok.hercules.elastic.adapter.handler.BulkHandler;
import ru.kontur.vostok.hercules.elastic.adapter.handler.IndexHandler;
import ru.kontur.vostok.hercules.elastic.adapter.index.IndexManager;
import ru.kontur.vostok.hercules.health.CommonMetrics;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.http.HttpServer;
import ru.kontur.vostok.hercules.http.handler.RouteHandler;
import ru.kontur.vostok.hercules.undertow.util.UndertowHttpServer;
import ru.kontur.vostok.hercules.http.handler.InstrumentedRouteHandlerBuilder;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class ElasticAdapterApplication {
    private static MetricsCollector metricsCollector;
    private static GateSender gateSender;
    private static IndexManager indexManager;

    public static void main(String[] args) {
        Application.run("Hercules Elastic Adapter", "elastic-adapter", args, (properties, container) -> {
            Properties metricsProperties = PropertiesUtil.ofScope(properties, Scopes.METRICS);
            Properties gateClientProperties = PropertiesUtil.ofScope(properties, Scopes.GATE_CLIENT);
            Properties indexManagerProperties = PropertiesUtil.ofScope(properties, "index.manager");
            Properties httpServerProperties = PropertiesUtil.ofScope(properties, Scopes.HTTP_SERVER);

            metricsCollector = container.register(new MetricsCollector(metricsProperties));
            CommonMetrics.registerCommonMetrics(metricsCollector);

            gateSender = container.register(new GateSender(gateClientProperties));

            indexManager = new IndexManager(indexManagerProperties);

            container.register(createHttpServer(httpServerProperties));
        });
    }

    private static HttpServer createHttpServer(Properties httpServerProperties) {
        RouteHandler handler = new InstrumentedRouteHandlerBuilder(httpServerProperties, metricsCollector).
                post("/_bulk", new BulkHandler(indexManager, gateSender)).
                post("/:index/_bulk", new BulkHandler(indexManager, gateSender)).
                post("/:index/_doc/_bulk", new BulkHandler(indexManager, gateSender)).
                post("/:index/_doc/", new IndexHandler(indexManager, gateSender)).
                build();

        return new UndertowHttpServer(
                Application.application().getConfig().getHost(),
                Application.application().getConfig().getPort(),
                httpServerProperties,
                handler);
    }
}
