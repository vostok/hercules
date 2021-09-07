package ru.kontur.vostok.hercules.graphite.adapter;

import ru.kontur.vostok.hercules.application.Application;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.graphite.adapter.purgatory.Accumulator;
import ru.kontur.vostok.hercules.graphite.adapter.purgatory.LegacyAccumulator;
import ru.kontur.vostok.hercules.graphite.adapter.purgatory.Purgatory;
import ru.kontur.vostok.hercules.graphite.adapter.server.GraphiteAdapterServer;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.undertow.util.servers.DaemonHttpServer;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class GraphiteAdapterApplication {
    public static void main(String[] args) {
        Application.run("Hercules Graphite Adapter", "graphite-adapter", args, (properties, container) -> {
            Properties metricsProperties = PropertiesUtil.ofScope(properties, Scopes.METRICS);
            Properties accumulatorProperties = PropertiesUtil.ofScope(properties, "accumulator");
            Properties purgatoryProperties = PropertiesUtil.ofScope(properties, "purgatory");
            Properties serverProperties = PropertiesUtil.ofScope(properties, "server");
            Properties httpServerProperties = PropertiesUtil.ofScope(properties, Scopes.HTTP_SERVER);

            MetricsCollector metricsCollector = container.register(new MetricsCollector(metricsProperties));

            Accumulator accumulator = container.register(new LegacyAccumulator(accumulatorProperties, metricsCollector));
            Purgatory purgatory = new Purgatory(purgatoryProperties, accumulator, metricsCollector);

            container.register(new GraphiteAdapterServer(serverProperties, purgatory, metricsCollector));

            container.register(new DaemonHttpServer(httpServerProperties, metricsCollector));
        });
    }
}
