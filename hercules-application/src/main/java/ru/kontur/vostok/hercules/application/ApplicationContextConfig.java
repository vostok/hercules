package ru.kontur.vostok.hercules.application;

import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class ApplicationContextConfig {
    private final String environment;
    private final String zone;
    private final String instanceId;

    public ApplicationContextConfig(Properties properties) {
        environment = PropertiesUtil.get(Props.ENVIRONMENT, properties).get();
        zone = PropertiesUtil.get(Props.ZONE, properties).get();
        instanceId = PropertiesUtil.get(Props.INSTANCE_ID, properties).get();
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
        static final Parameter<String> ENVIRONMENT =
                Parameter.stringParameter("environment").
                        required().
                        build();

        static final Parameter<String> ZONE =
                Parameter.stringParameter("zone").
                        required().
                        build();

        static final Parameter<String> INSTANCE_ID =
                Parameter.stringParameter("instance.id").
                        required().
                        build();

    }
}
