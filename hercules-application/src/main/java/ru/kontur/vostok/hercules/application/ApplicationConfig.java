package ru.kontur.vostok.hercules.application;

import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;
import ru.kontur.vostok.hercules.util.validation.Validators;

import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class ApplicationConfig {
    private final String host;
    private final int port;

    public ApplicationConfig(Properties properties) {
        host = Props.HOST.extract(properties);
        port = Props.PORT.extract(properties);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    private static class Props {
        static final PropertyDescription<String> HOST = PropertyDescriptions
                .stringProperty("host")
                .withDefaultValue(ApplicationConfigDefaults.DEFAULT_HOST)
                .build();

        static final PropertyDescription<Integer> PORT = PropertyDescriptions
                .integerProperty("port")
                .withDefaultValue(ApplicationConfigDefaults.DEFAULT_PORT)
                .withValidator(Validators.portValidator())
                .build();

    }
}
