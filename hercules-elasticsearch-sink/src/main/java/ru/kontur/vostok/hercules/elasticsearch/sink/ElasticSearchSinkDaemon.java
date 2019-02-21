package ru.kontur.vostok.hercules.elasticsearch.sink;

import ru.kontur.vostok.hercules.kafka.util.processing.bulk.AbstractBulkSinkDaemon;
import ru.kontur.vostok.hercules.kafka.util.processing.bulk.BulkSender;

import java.util.Properties;

@Deprecated
public class ElasticSearchSinkDaemon extends AbstractBulkSinkDaemon {

    public static void main(String[] args) {
        new ElasticSearchSinkDaemon().run(args);
    }

    @Override
    protected BulkSender createSender(Properties elasticsearchProperties) {
        return new ElasticSearchEventSender(elasticsearchProperties, super.metricsCollector);
    }

    @Override
    protected String getDaemonName() {
        return "Hercules elasticserch sink";
    }

    @Override
    protected String getDaemonId() {
        return "sink.elasticsearch";
    }
}
