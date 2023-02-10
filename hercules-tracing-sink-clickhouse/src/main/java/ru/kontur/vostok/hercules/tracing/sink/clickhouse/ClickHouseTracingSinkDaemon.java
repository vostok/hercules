package ru.kontur.vostok.hercules.tracing.sink.clickhouse;

import ru.kontur.vostok.hercules.application.Application;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.sink.AbstractSinkParallelDaemon;
import ru.kontur.vostok.hercules.sink.parallel.sender.NoPrepareParallelSender;

import java.util.Properties;

/**
 * Sink Daemon inserts tracing spans from traces stream into tracing spans table in ClickHouse.
 *
 * @author Gregory Koshelev
 * @see ClickHouseTracingSender
 * @see AbstractSinkParallelDaemon
 */
public class ClickHouseTracingSinkDaemon extends AbstractSinkParallelDaemon<NoPrepareParallelSender.NoPrepareEvents> {
    public static void main(String[] args) {
        Application.run(new ClickHouseTracingSinkDaemon(), args);
    }

    @Override
    protected NoPrepareParallelSender createSender(Properties senderProperties, MetricsCollector metricsCollector) {
        return new ClickHouseTracingSender(senderProperties, metricsCollector);
    }

    @Override
    public String getApplicationId() {
        return "sink.tracing-clickhouse";
    }

    @Override
    public String getApplicationName() {
        return "ClickHouse Tracing Sink";
    }
}
