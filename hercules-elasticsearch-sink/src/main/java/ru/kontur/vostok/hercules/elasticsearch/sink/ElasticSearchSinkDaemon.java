package ru.kontur.vostok.hercules.elasticsearch.sink;

import ru.kontur.vostok.hercules.kafka.util.processing.AbstractBulkSinkDaemon;
import ru.kontur.vostok.hercules.kafka.util.processing.BulkEventSender;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Map;
import java.util.Properties;

public class ElasticSearchSinkDaemon extends AbstractBulkSinkDaemon {

    public static void main(String[] args) {
        new ElasticSearchSinkDaemon().run(args);
    }

    @Override
    protected BulkEventSender createSender(Map<String, String> parameters) {
        Properties elasticsearchProperties = PropertiesUtil.readProperties(parameters.getOrDefault("elasticsearch.properties", "elasticsearch.properties"));

        return new ElasticSearchEventSender(elasticsearchProperties);
    }

    @Override
    protected String getDaemonName() {
        return "elasticsearch";
    }
}
