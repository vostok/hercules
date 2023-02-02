package ru.kontur.vostok.hercules.application;

import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.util.ClassUtil;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.validation.IntegerValidators;
import ru.kontur.vostok.hercules.util.validation.ValidationResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Configuration of the application.
 *
 * @author Gregory Koshelev
 */
public class ApplicationConfig {

    private final String host;
    private final int port;
    private final int shutdownTimeoutMs;
    private final int shutdownGracePeriodMs;
    private final List<ApplicationStateObserver> stateObservers;
    private final Properties allProperties;

    /**
     * Constructor.
     *
     * @param allProps All properties from the "application.properties" file.
     */
    public ApplicationConfig(Properties allProps) {
        this.allProperties = allProps;
        Properties appProps = PropertiesUtil.ofScope(allProps, Scopes.APPLICATION);
        this.host = PropertiesUtil.get(Props.HOST, appProps).get();
        this.port = PropertiesUtil.get(Props.PORT, appProps).get();
        this.shutdownTimeoutMs = PropertiesUtil.get(Props.SHUTDOWN_TIMEOUT_MS, appProps).get();
        this.shutdownGracePeriodMs = PropertiesUtil.get(Props.SHUTDOWN_GRACE_PERIOD_MS, appProps).get();
        this.stateObservers = Arrays.stream(PropertiesUtil.get(Props.STATE_OBSERVER_CLASSES, appProps).get())
                .map(className -> ClassUtil.fromClass(className, ApplicationStateObserver.class))
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Host of HTTP-server.
     *
     * @return Host of HTTP-server.
     * @deprecated Use {@code http.server.host} property instead {@code application.host}.
     */
    @Deprecated
    public String getHost() {
        return host;
    }

    /**
     * Port of HTTP-server.
     *
     * @return Port of HTTP-server.
     * @deprecated Use {@code http.server.port} property instead {@code application.port}.
     */
    @Deprecated
    public int getPort() {
        return port;
    }

    /**
     * Grace shutdown time budget.
     *
     * @return Grace shutdown time budget in milliseconds.
     */
    public int getShutdownGracePeriodMs() {
        return shutdownGracePeriodMs;
    }

    /**
     * Hard shutdown time budget.
     *
     * @return Hard shutdown time budget in milliseconds.
     */
    public int getShutdownTimeoutMs() {
        return shutdownTimeoutMs;
    }

    /**
     * Application state observers' instances listed in allProperties.
     *
     * @return Application state observers.
     */
    public List<ApplicationStateObserver> getStateObservers() {
        return stateObservers;
    }

    /**
     * All properties defined by the user.
     *
     * @return Properties.
     */
    public Properties getAllProperties() {
        return allProperties;
    }

    /**
     * Properties declarations of the {@link Scopes#APPLICATION "application"} scope.
     */
    public static class Props {

        /**
         * Application HTTP-server host.
         *
         * @deprecated Use "http.server.host" instead.
         */
        @Deprecated
        public static final Parameter<String> HOST = Parameter.stringParameter("host")
                .withDefault(ApplicationConfigDefaults.DEFAULT_HOST)
                .build();

        /**
         * Application HTTP-server port.
         *
         * @deprecated Use "http.server.port" instead.
         */
        @Deprecated
        public static final Parameter<Integer> PORT = Parameter.integerParameter("port")
                .withDefault(ApplicationConfigDefaults.DEFAULT_PORT)
                .withValidator(IntegerValidators.portValidator())
                .build();

        /**
         * Graceful shutdown time budget.
         */
        public static final Parameter<Integer> SHUTDOWN_GRACE_PERIOD_MS = Parameter.integerParameter("shutdown.grace.period.ms")
                .withDefault(ApplicationConfigDefaults.DEFAULT_SHUTDOWN_GRACE_PERIOD_MS)
                .withValidator(IntegerValidators.nonNegative())
                .build();

        /**
         * Hard shutdown time budget.
         */
        public static final Parameter<Integer> SHUTDOWN_TIMEOUT_MS = Parameter.integerParameter("shutdown.timeout.ms")
                .withDefault(ApplicationConfigDefaults.DEFAULT_SHUTDOWN_TIMEOUT_MS)
                .withValidator(IntegerValidators.nonNegative())
                .build();

        /**
         * Application state observers.
         * <p>
         * All classes must exist in classpath and implement {@link ApplicationStateObserver} interface.
         * </p>
         */
        public static final Parameter<String[]> STATE_OBSERVER_CLASSES = Parameter.stringArrayParameter("state.observers.classes")
                .withDefault(new String[0])
                .withValidator(array -> {
                    var doesntExists = new ArrayList<String>();
                    for (var clazz : array) {
                        try {
                            Class.forName(clazz);
                        } catch (ClassNotFoundException ex) {
                            doesntExists.add(clazz);
                        }
                    }
                    if (doesntExists.isEmpty()) {
                        return ValidationResult.ok();
                    }
                    return ValidationResult.error("classes " + String.join(", ", doesntExists) + " are not presented in classpath");
                })
                .build();

        private Props() {
            /* static class */
        }
    }
}
