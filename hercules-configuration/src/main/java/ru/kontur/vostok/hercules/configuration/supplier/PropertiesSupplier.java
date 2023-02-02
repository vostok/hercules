package ru.kontur.vostok.hercules.configuration.supplier;

import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;

/**
 * Interface of properties supplier.
 *
 * @author Aleksandr Yuferov
 */
@FunctionalInterface
public interface PropertiesSupplier extends Supplier<Properties> {

    /**
     * Command line argument name for properties-file path.
     */
    String FILE_ARG_NAME = "application.properties";

    /**
     * Default property-file path.
     */
    String FILE_ARG_DEFAULT_VALUE = "file://application.properties";

    /**
     * Get the default source of properties.
     *
     * @param cmdArgs Command line arguments.
     * @return Default properties source implementation.
     */
    static PropertiesSupplier defaultSupplier(Map<String, String> cmdArgs) {
        return new CombinedPropertiesSupplier(cmdArgs, new CmdArgsPropertiesSupplier(cmdArgs), new PropertiesFilePropertiesSupplier(cmdArgs));
    }
}
