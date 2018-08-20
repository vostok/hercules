package ru.kontur.vostok.hercules.util.application;

import ru.kontur.vostok.hercules.util.properties.PropertiesExtractor;

import java.util.Objects;
import java.util.Properties;

/**
 * ContextHolder stores context of application
 *
 * @author Kirill Sulim
 */
public class ContextHolder {

    private static volatile Context context;

    /**
     * Init application context. Must be called on startup if some parts of application uses this context
     * (such as MetricsCollector).
     *
     * @param applicationName application name
     * @param contextProperties context properties
     */
    public static void init(String applicationName, Properties contextProperties) {
        context = new Context(
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
    public static Context get() {
        if (Objects.isNull(context)) {
            throw new IllegalStateException("Context is nit initialized");
        }
        return context;
    }
}
