package ru.kontur.vostok.hercules.configuration.supplier;

import ru.kontur.vostok.hercules.configuration.PropertiesLoader;

import java.util.Map;
import java.util.Properties;

/**
 * '.properties'-file supplier.
 *
 * @author Aleksandr Yuferov
 */
public class PropertiesFilePropertiesSupplier implements PropertiesSupplier {

    private final Map<String, String> cmdArguments;

    public PropertiesFilePropertiesSupplier(Map<String, String> cmdArguments) {
        this.cmdArguments = cmdArguments;
    }

    @Override
    public Properties get() {
        String fileLink = cmdArguments.getOrDefault(FILE_ARG_NAME, FILE_ARG_DEFAULT_VALUE);
        return PropertiesLoader.load(fileLink);
    }
}
