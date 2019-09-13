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

    public ApplicationConfig(Properties properties) {
        host = PropertiesUtil.get(Props.HOST, properties).get();
        port = PropertiesUtil.get(Props.PORT, properties).get();
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
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

    }
}
