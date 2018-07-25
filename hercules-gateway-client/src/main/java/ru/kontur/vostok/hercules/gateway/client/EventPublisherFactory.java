package ru.kontur.vostok.hercules.gateway.client;

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
    // property fields
    private static final String URL = "url";
    private static final String THREADS = "threads";
    private static final String API_KEY = "apiKey";

    private static final int DEFAULT_THREADS_COUNT = 3;
    private static final String DEFAULT_RESOURCE_NAME = "gateway-client.properties";
    private static final ThreadFactory DEFAULT_THREAD_FACTORY = r -> {
        Thread thread = Executors.defaultThreadFactory().newThread(r);
        thread.setDaemon(true);
        return thread;
    };

    private static EventPublisher INSTANCE;

    static {
        String filename = System.getProperty("gateway.client.config");
        boolean fromResources = false;
        if (Objects.isNull(filename)) {
            filename = DEFAULT_RESOURCE_NAME;
            fromResources = true;
        }
        try {
            Properties properties = loadProperties(filename, fromResources);
            INSTANCE = createPublisher(properties);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private EventPublisherFactory() {

    }

    public static EventPublisher getInstance() {
        return INSTANCE;
    }

    private static EventPublisher createPublisher(Properties properties) {
        int threads = PropertiesUtil.get(properties, THREADS, DEFAULT_THREADS_COUNT);
        String url = PropertiesUtil
                .getAs(properties, URL, String.class)
                .orElseThrow(PropertiesUtil.missingPropertyError(URL));
        String apiKey = PropertiesUtil
                .getAs(properties, API_KEY, String.class)
                .orElseThrow(PropertiesUtil.missingPropertyError(API_KEY));

        return new EventPublisher(
                threads,
                DEFAULT_THREAD_FACTORY,
                Collections.emptyList(),
                url,
                apiKey
        );
    }

    private static Properties loadProperties(String filename, boolean fromResources) throws IOException {
        Properties properties = new Properties();
        InputStream inputStream;

        if (fromResources) {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            inputStream = loader.getResourceAsStream(filename);
        } else {
            inputStream = new FileInputStream(filename);
        }

        properties.load(inputStream);

        return properties;
    }
}
