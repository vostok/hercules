package ru.kontur.vostok.hercules.gateway.client;

import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.Executors;

/**
 * @author Daniil Zhenikhov
 */
public class EventPublisherFactory {
    private static final int DEFAULT_THREADS_COUNT = 3;
    private static final String DEFAULT_RESOURCE_NAME = "gateway-client.properties";
    private static final Properties PROPERTIES = new Properties();

    private static EventPublisher INSTANCE;

    static {
        String filename = System.getProperty("gateway.client.config", DEFAULT_RESOURCE_NAME);
        try {
            FileInputStream fileInputStream = new FileInputStream(filename);
            PROPERTIES.load(fileInputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static EventPublisher create() {
        if (Objects.isNull(INSTANCE)) {
            checkRequiredFields();

            //TODO: remove extra param - loseOnOverflow
            return new EventPublisher(
                    PropertiesUtil.get(PROPERTIES, "threads", DEFAULT_THREADS_COUNT),
                    Executors.defaultThreadFactory(),
                    Collections.emptyList(),
                    PROPERTIES.getProperty("url"),
                    PROPERTIES.getProperty("apiKey")
            );
        } else {
            return INSTANCE;
        }
    }

    private static void checkRequiredFields() {
        if (!EventPublisherFactory.PROPERTIES.containsKey("url")
                || !EventPublisherFactory.PROPERTIES.containsKey("apiKey")) {
            throw new IllegalArgumentException("Missing required property ('url', 'apiKey')");
        }
    }

    private EventPublisherFactory() {

    }
}
