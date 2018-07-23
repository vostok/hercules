package ru.kontur.vostok.hercules.gateway.client;

import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Properties;

/**
 * @author Daniil Zhenikhov
 */
public class EventPublisherConfigurationFactory {
    public static void checkRequiredFields(Properties properties) {
        if (!properties.containsKey("url")
                && !properties.containsKey("apiKey")) {
            throw new IllegalStateException("Missing required property ('url', 'apiKey')");
        }
    }

    public EventPublisherConfiguration create(String url, String apiKey) {
        return new EventPublisherConfiguration(
                EventPublisherConfiguration.DefaultConfiguration.getPeriodMillis(),
                EventPublisherConfiguration.DefaultConfiguration.getBatchSize(),
                EventPublisherConfiguration.DefaultConfiguration.getThreads(),
                EventPublisherConfiguration.DefaultConfiguration.getCapacity(),
                url,
                apiKey
        );
    }

    public EventPublisherConfiguration create(Properties properties) {
        checkRequiredFields(properties);

        return create(
                properties.getProperty("url"),
                properties.getProperty("apiKey"),
                properties
        );
    }

    public EventPublisherConfiguration create(String url, String apiKey, Properties properties) {
        return new EventPublisherConfiguration(
                PropertiesUtil.get(
                        properties,
                        "periodMillis",
                        EventPublisherConfiguration.DefaultConfiguration.getPeriodMillis()),
                PropertiesUtil.get(
                        properties,
                        "batchSize",
                        EventPublisherConfiguration.DefaultConfiguration.getBatchSize()),
                PropertiesUtil.get(
                        properties,
                        "threads",
                        EventPublisherConfiguration.DefaultConfiguration.getThreads()),
                PropertiesUtil.get(
                        properties,
                        "capacity",
                        EventPublisherConfiguration.DefaultConfiguration.getCapacity()),
                url,
                apiKey
        );
    }
}
