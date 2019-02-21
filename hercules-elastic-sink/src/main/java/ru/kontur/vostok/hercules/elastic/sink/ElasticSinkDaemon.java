package ru.kontur.vostok.hercules.elastic.sink;

import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.sink.AbstractSinkDaemon;
import ru.kontur.vostok.hercules.sink.Sender;

import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class ElasticSinkDaemon extends AbstractSinkDaemon {
    public static void main(String[] args) {
        new ElasticSinkDaemon().run(args);
    }

    @Override
    protected Sender createSender(Properties elasticProperties, MetricsCollector metricsCollector) {
        return new ElasticSender(elasticProperties, metricsCollector);
    }

    @Override
    protected String getDaemonId() {
        return "sink.elastic";
    }

    @Override
    protected String getDaemonName() {
        return "Hercules Elastic Sink";
    }
}
