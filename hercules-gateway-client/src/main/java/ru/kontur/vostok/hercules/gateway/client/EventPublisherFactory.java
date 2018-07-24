package ru.kontur.vostok.hercules.gateway.client;

import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import javax.swing.*;
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
    private static final int DEFAULT_THREADS_COUNT = 3;
    private static final String DEFAULT_RESOURCE_NAME = "gateway-client.properties";
    private static final ThreadFactory DEFAULT_THREAD_FACTORY = r -> {
        Thread thread = Executors.defaultThreadFactory().newThread(r);
        thread.setDaemon(true);
        return thread;
    };

    private static EventPublisher INSTANCE;

    static {
        String filename = System.getProperty("gateway.client.config", DEFAULT_RESOURCE_NAME);
        try {
            Properties properties = new Properties();
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            InputStream fileInputStream = loader.getResourceAsStream(filename);
            properties.load(fileInputStream);

            INSTANCE = create(properties);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static EventPublisher getInstance() {
        return INSTANCE;
    }

    private static EventPublisher create(Properties properties) {
        checkRequiredFields(properties);

        return new EventPublisher(
                PropertiesUtil.get(properties, "threads", DEFAULT_THREADS_COUNT),
                DEFAULT_THREAD_FACTORY,
                Collections.emptyList(),
                properties.getProperty("url"),
                properties.getProperty("apiKey")
        );
    }

    private static void checkRequiredFields(Properties properties) {
        if (!properties.containsKey("url") || !properties.containsKey("apiKey")) {
            throw new IllegalArgumentException("Missing required property ('url', 'apiKey')");
        }
    }

    private EventPublisherFactory() {

    }
}
