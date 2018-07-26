package ru.kontur.vostok.hercules.gateway.client;

import ru.kontur.vostok.hercules.util.properties.ConfigsUtil;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * @author Daniil Zhenikhov
 */
public class EventPublisherFactory {
    private static final String URL_PROPERTY = "url";
    private static final String THREADS_PROPERTY = "threads";
    private static final String API_KEY_PROPERTY = "apiKey";

    private static final String DEFAULT_RESOURCE_NAME = "gateway-client.properties";
    private static final String PROPERTY_NAME = "gateway.client.config";
    private static final int DEFAULT_THREADS_COUNT = 3;
    private static final ThreadFactory DEFAULT_THREAD_FACTORY = r -> {
        Thread thread = Executors.defaultThreadFactory().newThread(r);
        thread.setDaemon(true);
        return thread;
    };

    private static EventPublisher INSTANCE;

    static {
        InputStream inputStream = ConfigsUtil.readConfig(PROPERTY_NAME, DEFAULT_RESOURCE_NAME);
        try {
            INSTANCE = createPublisher(loadProperties(inputStream));
        } catch (IOException e) {
            throw new RuntimeException(e);
        };
    }

    private EventPublisherFactory() {

    }

    public static EventPublisher getInstance() {
        return INSTANCE;
    }

    private static EventPublisher createPublisher(Properties properties) {
        int threads = PropertiesUtil.get(properties, THREADS_PROPERTY, DEFAULT_THREADS_COUNT);
        String url = PropertiesUtil
                .getAs(properties, URL_PROPERTY, String.class)
                .orElseThrow(PropertiesUtil.missingPropertyError(URL_PROPERTY));
        String apiKey = PropertiesUtil
                .getAs(properties, API_KEY_PROPERTY, String.class)
                .orElseThrow(PropertiesUtil.missingPropertyError(API_KEY_PROPERTY));

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
