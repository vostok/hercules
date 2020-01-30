package ru.kontur.vostok.hercules.tracing.sink.clickhouse;

import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.sink.AbstractSinkDaemon;
import ru.kontur.vostok.hercules.sink.Sender;

import java.util.Properties;

/**
 * Sink Daemon insert tracing spans from traces stream into tracing spans table in ClickHouse.
 *
 * @author Gregory Koshelev
 * @see ClickHouseTracingSender
 * @see AbstractSinkDaemon
 */
public class ClickHouseTracingSinkDaemon extends AbstractSinkDaemon {
    public static void main(String[] args) {
        new ClickHouseTracingSinkDaemon().run(args);
    }

    @Override
    protected Sender createSender(Properties senderProperties, MetricsCollector metricsCollector) {
        return new ClickHouseTracingSender(senderProperties, metricsCollector);
    }

    @Override
    protected String getDaemonId() {
        return "sink.tracing.clickhouse";
    }

    @Override
    protected String getDaemonName() {
        return "ClickHouse Tracing Sink";
    }
}
