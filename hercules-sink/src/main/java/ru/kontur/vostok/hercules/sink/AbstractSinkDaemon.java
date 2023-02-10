package ru.kontur.vostok.hercules.sink;

import ru.kontur.vostok.hercules.application.Application;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.health.CommonMetrics;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.undertow.util.servers.DaemonHttpServer;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Properties;

/**
 * Base implementation of Sink daemon. Uses pool of SenderSink for concurrent processing.
 *
 * @author Gregory Koshelev
 */
public abstract class AbstractSinkDaemon {
    protected void run(String[] args) {
        run(args, (properties, container) -> {
        });
    }

    protected void run(String[] args, Application.Starter starter) {
        Application.run(getDaemonName(), getDaemonId(), args, (properties, container) -> {
            Properties metricsProperties = PropertiesUtil.ofScope(properties, Scopes.METRICS);
            Properties httpServerProperties = PropertiesUtil.ofScope(properties, Scopes.HTTP_SERVER);
            Properties sinkProperties = PropertiesUtil.ofScope(properties, Scopes.SINK);
            Properties senderProperties = PropertiesUtil.ofScope(sinkProperties, Scopes.SENDER);

            starter.start(properties, container);

            MetricsCollector metricsCollector = container.register(new MetricsCollector(metricsProperties));
            CommonMetrics.registerCommonMetrics(metricsCollector);
            Sender sender = container.register(createSender(senderProperties, metricsCollector));

            new BaseSinkDaemonStarter(sender, getDaemonId(), metricsCollector).start(properties, container);

            container.register(new DaemonHttpServer(httpServerProperties, metricsCollector));
        });
    }

    protected abstract Sender createSender(Properties senderProperties, MetricsCollector metricsCollector);

    /**
     * Application id is used to identify across Hercules Cluster. E.g. in metrics, logging and others.
     *
     * @return application id
     */
    protected abstract String getDaemonId();

    /**
     * Human readable application name.
     *
     * @return application name
     */
    protected abstract String getDaemonName();
}
