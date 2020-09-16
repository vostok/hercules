package ru.kontur.vostok.hercules.gate.client;

import ru.kontur.vostok.hercules.util.Lazy;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.ConfigsUtil;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Optional;
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
    private static final String SUBPROJECT;
    private static final String ENVIRONMENT;

    static {
        InputStream inputStream = ConfigsUtil.readConfig(PROPERTY_NAME, DEFAULT_RESOURCE_NAME);
        try {
            Properties properties = loadProperties(inputStream);

            LAZY_INSTANCE = new Lazy<>(() -> createPublisher(properties));

            PROJECT = PropertiesUtil.get(Props.PROJECT, properties).get();
            SUBPROJECT = PropertiesUtil.get(Props.SUBPROJECT, properties).get();
            ENVIRONMENT = PropertiesUtil.get(Props.ENVIRONMENT, properties).get();
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

    public static Optional<String> getSubproject() {
        return Optional.ofNullable(SUBPROJECT);
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
        static final Parameter<String> PROJECT =
                Parameter.stringParameter("project").
                        required().
                        build();

        static final Parameter<String> SUBPROJECT =
                Parameter.stringParameter("subproject").
                        build();

        static final Parameter<String> ENVIRONMENT =
                Parameter.stringParameter("environment").
                        required().
                        build();
    }
}
