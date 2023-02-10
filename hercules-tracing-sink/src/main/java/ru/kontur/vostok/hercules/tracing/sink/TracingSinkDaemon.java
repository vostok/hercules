package ru.kontur.vostok.hercules.tracing.sink;

import ru.kontur.vostok.hercules.application.Application;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.sink.AbstractSinkParallelDaemon;
import ru.kontur.vostok.hercules.sink.parallel.sender.NoPrepareParallelSender;

import java.util.Properties;

/**
 * Sink Daemon processes traces stream to traces table in Cassandra.
 * <p>
 * See {@link TracingSender} for details.
 *
 * @author Gregory Koshelev
 * @see TracingSender
 * @see AbstractSinkParallelDaemon
 */
public class TracingSinkDaemon extends AbstractSinkParallelDaemon<NoPrepareParallelSender.NoPrepareEvents> {
    public static void main(String[] args) {
        Application.run(new TracingSinkDaemon(), args);
    }

    @Override
    protected NoPrepareParallelSender createSender(Properties senderProperties, MetricsCollector metricsCollector) {
        return new TracingSender(senderProperties, metricsCollector);
    }

    @Override
    public String getApplicationId() {
        return "sink.tracing";
    }

    @Override
    public String getApplicationName() {
        return "Tracing Sink";
    }
}
