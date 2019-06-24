package ru.kontur.vostok.hercules.tracing.sink;

import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.sink.AbstractSinkDaemon;
import ru.kontur.vostok.hercules.sink.Sender;

import java.util.Properties;

/**
 * Sink Daemon processes traces stream to traces table in Cassandra.
 * <p>
 * See {@link TracingSender} for details.
 *
 * @author Gregory Koshelev
 * @see TracingSender
 * @see AbstractSinkDaemon
 */
public class TracingSinkDaemon extends AbstractSinkDaemon {
    public static void main(String[] args) {
        new TracingSinkDaemon().run(args);
    }

    @Override
    protected Sender createSender(Properties senderProperties, MetricsCollector metricsCollector) {
        return new TracingSender(senderProperties, metricsCollector);
    }

    @Override
    protected String getDaemonId() {
        return "sink.tracing";
    }

    @Override
    protected String getDaemonName() {
        return "Tracing Sink";
    }
}
