package ru.kontur.vostok.hercules.configuration.supplier;

import java.util.Map;
import java.util.Properties;

/**
 * Properties supplier that parses command line arguments.
 *
 * @author Aleksandr Yuferov
 */
public class CmdArgsPropertiesSupplier implements PropertiesSupplier {

    private final Map<String, String> cmdArgs;

    public CmdArgsPropertiesSupplier(Map<String, String> cmdArgs) {
        this.cmdArgs = cmdArgs;
    }

    @Override
    public Properties get() {
        Properties properties = new Properties();
        properties.putAll(cmdArgs);
        return properties;
    }
}
