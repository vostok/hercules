package ru.kontur.vostok.hercules.elasticsearch.sink;

import ru.kontur.vostok.hercules.kafka.util.processing.AbstractBulkSinkDaemon;
import ru.kontur.vostok.hercules.kafka.util.processing.BulkSender;

import java.util.Properties;

public class ElasticSearchSinkDaemon extends AbstractBulkSinkDaemon {

    public static void main(String[] args) {
        new ElasticSearchSinkDaemon().run(args);
    }

    @Override
    protected BulkSender createSender(Properties elasticsearchProperties) {
        return new ElasticSearchEventSender(elasticsearchProperties);
    }

    @Override
    protected String getDaemonName() {
        return "elasticsearch";
    }
}
