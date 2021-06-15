package ru.kontur.vostok.hercules.application;

import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.validation.IntegerValidators;

import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class ApplicationConfig {
    private final String host;
    private final int port;
    private final int shutdownTimeoutMs;
    private final int shutdownGracePeriodMs;

    public ApplicationConfig(Properties properties) {
        host = PropertiesUtil.get(Props.HOST, properties).get();
        port = PropertiesUtil.get(Props.PORT, properties).get();
        shutdownTimeoutMs = PropertiesUtil.get(Props.SHUTDOWN_TIMEOUT_MS, properties).get();
        shutdownGracePeriodMs = PropertiesUtil.get(Props.SHUTDOWN_GRACE_PERIOD_MS, properties).get();
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getShutdownTimeoutMs() {
        return shutdownTimeoutMs;
    }

    public int getShutdownGracePeriodMs() {
        return shutdownGracePeriodMs;
    }

    private static class Props {
        static final Parameter<String> HOST =
                Parameter.stringParameter("host").
                        withDefault(ApplicationConfigDefaults.DEFAULT_HOST).
                        build();

        static final Parameter<Integer> PORT =
                Parameter.integerParameter("port").
                        withDefault(ApplicationConfigDefaults.DEFAULT_PORT).
                        withValidator(IntegerValidators.portValidator()).
                        build();

        static final Parameter<Integer> SHUTDOWN_TIMEOUT_MS =
                Parameter.integerParameter("shutdown.timeout.ms").
                        withDefault(ApplicationConfigDefaults.DEFAULT_SHUTDOWN_TIMEOUT_MS).
                        withValidator(IntegerValidators.nonNegative()).
                        build();

        static final Parameter<Integer> SHUTDOWN_GRACE_PERIOD_MS =
                Parameter.integerParameter("shutdown.grace.period.ms").
                        withDefault(ApplicationConfigDefaults.DEFAULT_SHUTDOWN_GRACE_PERIOD_MS).
                        withValidator(IntegerValidators.nonNegative()).
                        build();
    }
}
