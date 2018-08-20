package ru.kontur.vostok.hercules.util.application;

import ru.kontur.vostok.hercules.util.properties.PropertiesExtractor;

import java.util.Objects;
import java.util.Properties;

/**
 * ContextHolder
 *
 * @author Kirill Sulim
 */
public class ContextHolder {

    private static volatile Context context;

    public static void init(String applicationName, Properties contextProperties) {
        context = new Context(
                applicationName,
                PropertiesExtractor.getRequiredProperty(contextProperties, "environment", String.class),
                PropertiesExtractor.getRequiredProperty(contextProperties, "instance.id", String.class)
        );
    }

    public static Context get() {
        if (Objects.isNull(context)) {
            throw new IllegalStateException("Context is nit initialized");
        }
        return context;
    }
}
