package ru.kontur.vostok.hercules.configuration.supplier;

import java.util.Map;
import java.util.Properties;

/**
 * Properties supplier that combines {@link PropertiesFilePropertiesSupplier '.properties'-files} supplier and
 * {@link CmdArgsPropertiesSupplier cmd arguments} supplier.
 *
 * @author Aleksandr Yuferov
 */
public class CombinedPropertiesSupplier implements PropertiesSupplier {

    private final Map<String, String> cmdArgs;
    private final PropertiesSupplier cmdArgsPropertiesSupplier;
    private final PropertiesSupplier propertiesFilePropertiesSupplier;

    /**
     * Constructor.
     *
     * @param cmdArgs                          Command line arguments.
     * @param cmdArgsPropertiesSupplier        {@link PropertiesSupplier} that takes properties from command line arguments.
     * @param propertiesFilePropertiesSupplier {@link PropertiesSupplier} that takes properties from properties-files.
     */
    public CombinedPropertiesSupplier(
            Map<String, String> cmdArgs,
            PropertiesSupplier cmdArgsPropertiesSupplier,
            PropertiesSupplier propertiesFilePropertiesSupplier
    ) {
        this.cmdArgs = cmdArgs;
        this.cmdArgsPropertiesSupplier = cmdArgsPropertiesSupplier;
        this.propertiesFilePropertiesSupplier = propertiesFilePropertiesSupplier;
    }

    /**
     * Perform loading.
     * <p>
     * Loads properties file specified by command line argument "application.properties".
     * If there is no such command line argument then will be used default value "file://application.properties".
     * </p>
     * <p>
     * For properties files that have extension ".properties" will be invoked {@code propertiesFilePropertiesSource}.
     * {@link IllegalArgumentException} will be thrown if file have any other extension.
     * </p>
     * <p>
     * After file properties loaded performing parsing properties from command line arguments delegating this work to {@code cmdArgsPropertiesSource}.
     * </p>
     *
     * @return Combined properties from multiple sources.
     */
    @Override
    public Properties get() {
        String fileName = cmdArgs.getOrDefault(FILE_ARG_NAME, FILE_ARG_DEFAULT_VALUE);
        Properties result = new Properties();
        if (fileName.endsWith(".properties")) {
            result.putAll(propertiesFilePropertiesSupplier.get());
        } else {
            throw new IllegalArgumentException(
                    String.format("unsupported properties file extension: \"%s\" (supported extensions are: properties)", fileName)
            );
        }
        result.putAll(cmdArgsPropertiesSupplier.get());
        return result;
    }
}
