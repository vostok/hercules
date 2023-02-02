package ru.kontur.vostok.hercules.application;

import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Properties;

/**
 * Application context config (declared in {@link Scopes#CONTEXT "context"} property scope).
 *
 * @author Gregory Koshelev
 */
public class ApplicationContextConfig {

    private final String environment;
    private final String zone;
    private final String instanceId;

    /**
     * Constructor.
     *
     * @param allProperties Properties.
     */
    public ApplicationContextConfig(Properties allProperties) {
        Properties properties = PropertiesUtil.ofScope(allProperties, Scopes.CONTEXT);
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

    /**
     * Properties declarations of the {@link Scopes#CONTEXT "context"} scope.
     */
    public static class Props {

        /**
         * Environment name.
         */
        public static final Parameter<String> ENVIRONMENT =
                Parameter.stringParameter("environment")
                        .required()
                        .build();

        /**
         * Zone name.
         */
        public static final Parameter<String> ZONE =
                Parameter.stringParameter("zone")
                        .required()
                        .build();

        /**
         * Instance id.
         */
        public static final Parameter<String> INSTANCE_ID =
                Parameter.stringParameter("instance.id")
                        .required()
                        .build();

        private Props() {
            /* static class */
        }
    }
}
