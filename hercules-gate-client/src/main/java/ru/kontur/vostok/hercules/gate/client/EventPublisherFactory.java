package ru.kontur.vostok.hercules.gate.client;

import ru.kontur.vostok.hercules.util.Lazy;
import ru.kontur.vostok.hercules.util.properties.ConfigsUtil;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * @author Daniil Zhenikhov
 */
public class EventPublisherFactory {

    private static final String DEFAULT_RESOURCE_NAME = "hercules-gate-client.properties";
    private static final String PROPERTY_NAME = "hercules.gate.client.config";

    private static final ThreadFactory DEFAULT_THREAD_FACTORY = r -> {
        Thread thread = Executors.defaultThreadFactory().newThread(r);
        thread.setDaemon(true);
        return thread;
    };

    private static final Lazy<EventPublisher> LAZY_INSTANCE;
    private static final String PROJECT;
    private static final String ENVIRONMENT;

    static {
        InputStream inputStream = ConfigsUtil.readConfig(PROPERTY_NAME, DEFAULT_RESOURCE_NAME);
        try {
            Properties properties = loadProperties(inputStream);

            LAZY_INSTANCE = new Lazy<>(() -> createPublisher(properties));

            PROJECT = Props.PROJECT.extract(properties);
            ENVIRONMENT = Props.ENVIRONMENT.extract(properties);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private EventPublisherFactory() {

    }

    public static EventPublisher getInstance() {
        return LAZY_INSTANCE.get();
    }

    public static String getProject() {
        return PROJECT;
    }

    public static String getEnvironment() {
        return ENVIRONMENT;
    }

    private static EventPublisher createPublisher(Properties properties) {
        return new EventPublisher(
                properties,
                DEFAULT_THREAD_FACTORY,
                Collections.emptyList()
        );
    }

    private static Properties loadProperties(InputStream inputStream) throws IOException {
        Properties properties = new Properties();
        properties.load(inputStream);
        return properties;
    }

    private static class Props {
        static final PropertyDescription<String> PROJECT =
                PropertyDescriptions
                        .stringProperty("project")
                        .build();

        static final PropertyDescription<String> ENVIRONMENT =
                PropertyDescriptions
                        .stringProperty("env")
                        .build();
    }
}
