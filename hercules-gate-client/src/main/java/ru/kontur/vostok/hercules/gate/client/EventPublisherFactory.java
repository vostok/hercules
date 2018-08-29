package ru.kontur.vostok.hercules.gate.client;

import ru.kontur.vostok.hercules.util.properties.ConfigsUtil;
import ru.kontur.vostok.hercules.util.properties.PropertiesExtractor;

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
    private static final String URLS_PROPERTY = "urls";
    private static final String THREADS_PROPERTY = "threads";
    private static final String API_KEY_PROPERTY = "apiKey";
    private static final String PROJECT_PROPERTY = "project";
    private static final String ENVIRONMENT_PROPERTY = "env";

    private static final String DEFAULT_RESOURCE_NAME = "hercules-gate-client.properties";
    private static final String PROPERTY_NAME = "hercules.gate.client.config";
    private static final int DEFAULT_THREADS_COUNT = 3;
    private static final ThreadFactory DEFAULT_THREAD_FACTORY = r -> {
        Thread thread = Executors.defaultThreadFactory().newThread(r);
        thread.setDaemon(true);
        return thread;
    };

    private static final EventPublisher INSTANCE;
    private static final String PROJECT;
    private static final String ENVIRONMENT;

    static {
        InputStream inputStream = ConfigsUtil.readConfig(PROPERTY_NAME, DEFAULT_RESOURCE_NAME);
        try {
            Properties properties = loadProperties(inputStream);
            INSTANCE = createPublisher(properties);
            PROJECT = PropertiesExtractor
                    .getAs(properties, PROJECT_PROPERTY, String.class)
                    .orElseThrow(PropertiesExtractor.missingPropertyError(PROJECT_PROPERTY));
            ENVIRONMENT = PropertiesExtractor
                    .getAs(properties, ENVIRONMENT_PROPERTY, String.class)
                    .orElseThrow(PropertiesExtractor.missingPropertyError(ENVIRONMENT_PROPERTY));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ;
    }

    private EventPublisherFactory() {

    }

    public static EventPublisher getInstance() {
        return INSTANCE;
    }

    public static String getProject() {
        return PROJECT;
    }

    public static String getEnvironment() {
        return ENVIRONMENT;
    }

    private static EventPublisher createPublisher(Properties properties) {
        int threads = PropertiesExtractor.get(properties, THREADS_PROPERTY, DEFAULT_THREADS_COUNT);
        String[] url = PropertiesExtractor
                .getAs(properties, URLS_PROPERTY, String.class)
                .orElseThrow(PropertiesExtractor.missingPropertyError(URLS_PROPERTY))
                .split("\\s*,\\s*");
        String apiKey = PropertiesExtractor
                .getAs(properties, API_KEY_PROPERTY, String.class)
                .orElseThrow(PropertiesExtractor.missingPropertyError(API_KEY_PROPERTY));


        return new EventPublisher(
                threads,
                DEFAULT_THREAD_FACTORY,
                Collections.emptyList(),
                url,
                apiKey
        );
    }

    private static Properties loadProperties(InputStream inputStream) throws IOException {
        Properties properties = new Properties();
        properties.load(inputStream);
        return properties;
    }
}
