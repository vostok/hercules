package ru.kontur.vostok.hercules.util.application;

import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;

import java.util.Objects;
import java.util.Properties;

/**
 * ApplicationContextHolder stores context of application
 *
 * @author Kirill Sulim
 */
public class ApplicationContextHolder {

    private static class Props {
        static final PropertyDescription<String> ENVIRONMENT = PropertyDescriptions
                .stringProperty("environment")
                .build();

        static final PropertyDescription<String> INSTANCE_ID = PropertyDescriptions
                .stringProperty("instance.id")
                .build();
    }

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
                Props.ENVIRONMENT.extract(contextProperties),
                Props.INSTANCE_ID.extract(contextProperties)
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
