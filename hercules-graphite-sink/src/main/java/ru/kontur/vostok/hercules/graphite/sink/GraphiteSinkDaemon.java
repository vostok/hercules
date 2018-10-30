package ru.kontur.vostok.hercules.graphite.sink;

import ru.kontur.vostok.hercules.kafka.util.processing.bulk.AbstractBulkSinkDaemon;
import ru.kontur.vostok.hercules.kafka.util.processing.bulk.BulkSender;
import ru.kontur.vostok.hercules.protocol.Event;

import java.util.Properties;

public class GraphiteSinkDaemon extends AbstractBulkSinkDaemon {

    public static void main(String[] args) {
        new GraphiteSinkDaemon().run(args);
    }

    @Override
    protected BulkSender<Event> createSender(Properties graphiteProperties) {
        return new GraphiteEventSender(graphiteProperties, this.metricsCollector);
    }

    @Override
    protected String getDaemonName() {
        return "Hercules graphite sink";
    }

    @Override
    protected String getDaemonId() {
        return "sink.graphite";
    }
}
