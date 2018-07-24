package ru.kontur.vostok.hercules.graphite.sink;

import ru.kontur.vostok.hercules.kafka.util.processing.AbstractBulkSinkDaemon;
import ru.kontur.vostok.hercules.kafka.util.processing.BulkSender;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Map;
import java.util.Properties;

public class GraphiteSinkDaemon extends AbstractBulkSinkDaemon {

    public static void main(String[] args) {
        new GraphiteSinkDaemon().run(args);
    }

    @Override
    protected BulkSender<Event> createSender(Map<String, String> parameters) {
        Properties graphiteProperties = PropertiesUtil.readProperties(parameters.getOrDefault("graphite.properties", "graphite.properties"));

        return new GraphiteEventSender(graphiteProperties);
    }

    @Override
    protected String getDaemonName() {
        return "graphite";
    }
}
