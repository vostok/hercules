package ru.kontur.vostok.hercules.sentry.sink;

import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.sink.AbstractSinkDaemon;

import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class SentrySinkDaemon extends AbstractSinkDaemon {

    /**
     * Main starting point
     */
    public static void main(String[] args) {
        new SentrySinkDaemon().run(args);
    }

    @Override
    protected SentrySender createSender(Properties senderProperties, MetricsCollector metricsCollector) {
        return new SentrySender(senderProperties, metricsCollector);
    }

    @Override
    protected String getDaemonName() {
        return "Hercules sentry sink";
    }

    @Override
    protected String getDaemonId() {
        return "sink.sentry";
    }
}
