package ru.kontur.vostok.hercules.application;

import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;

import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class ApplicationContextConfig {
    private final String environment;
    private final String zone;
    private final String instanceId;

    public ApplicationContextConfig(Properties properties) {
        environment = Props.ENVIRONMENT.extract(properties);
        zone = Props.ZONE.extract(properties);
        instanceId = Props.INSTANCE_ID.extract(properties);
    }

    public String getEnvironment() {
        return environment;
    }

    public String getZone() {
        return zone;
    }

    public String getInstanceId() {
        return instanceId;
    }

    private static class Props {
        static final PropertyDescription<String> ENVIRONMENT = PropertyDescriptions
                .stringProperty("environment")
                .build();

        static final PropertyDescription<String> ZONE = PropertyDescriptions
                .stringProperty("zone")
                .build();

        static final PropertyDescription<String> INSTANCE_ID = PropertyDescriptions
                .stringProperty("instance.id")
                .build();
    }
}
