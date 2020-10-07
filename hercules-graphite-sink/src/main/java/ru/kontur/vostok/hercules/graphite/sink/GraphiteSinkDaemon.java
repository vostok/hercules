package ru.kontur.vostok.hercules.graphite.sink;

import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.sink.AbstractSinkDaemon;
import ru.kontur.vostok.hercules.sink.Sender;

import java.util.Properties;

public class GraphiteSinkDaemon extends AbstractSinkDaemon {
    public static void main(String[] args) {
        new GraphiteSinkDaemon().run(args);
    }

    @Override
    protected Sender createSender(Properties properties, MetricsCollector metricsCollector) {
        return new GraphiteSender(properties, metricsCollector);
    }

    @Override
    protected String getDaemonId() {
        return "sink.graphite";
    }

    @Override
    protected String getDaemonName() {
        return "Hercules Graphite Sink";
    }
}
