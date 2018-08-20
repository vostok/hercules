package ru.kontur.vostok.hercules.util.application;

import ru.kontur.vostok.hercules.util.properties.PropertiesExtractor;

import java.util.Objects;
import java.util.Properties;

/**
 * ApplicationContextHolder stores context of application
 *
 * @author Kirill Sulim
 */
public class ApplicationContextHolder {

    private static volatile ApplicationContext applicationContext;

    /**
     * Init application context. Must be called on startup if some parts of application uses this context
     * (such as MetricsCollector).
     *
     * @param applicationName application name
     * @param contextProperties context properties
     */
    public static void init(String applicationName, Properties contextProperties) {
        applicationContext = new ApplicationContext(
                applicationName,
                PropertiesExtractor.getRequiredProperty(contextProperties, "environment", String.class),
                PropertiesExtractor.getRequiredProperty(contextProperties, "instance.id", String.class)
        );
    }

    /**
     * Get application context
     *
     * @return application context
     */
    public static ApplicationContext get() {
        if (Objects.isNull(applicationContext)) {
            throw new IllegalStateException("Context is not initialized");
        }
        return applicationContext;
    }
}
