package ru.kontur.vostok.hercules.util.properties;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Objects;

/**
 * @author Daniil Zhenikhov
 */
public final class ConfigsUtil {
    /**
     * Read config file.
     * <br>If propertyName exists then try to read from path specified in property.
     * <br>Else try to load from resources.
     * @param propertyName name of property containing path to config file
     * @param defaultResource name of default resources containing config file
     * @return {@link java.io.InputStream input stream} of config file
     * @throws java.lang.RuntimeException wrapping exception that was occurred while trying to load config
     */
    public static InputStream readConfig(String propertyName, String defaultResource) {
        String filename = System.getProperty(propertyName);
        boolean fromResources = false;
        if (Objects.isNull(filename)) {
            filename = defaultResource;
            fromResources = true;
        }
        try {
            return readConfig(filename, fromResources);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Read config file.
     * <br>
     *
     * @param filename name of config file. May be resource or path to config file.
     * @param fromResources flag that config file is resource or not
     * @return {@link java.io.InputStream input stream} of config file
     * @throws FileNotFoundException if file is not resource and has not found, exception will be thrown
     */
    public static InputStream readConfig(String filename, boolean fromResources) throws FileNotFoundException {
        if (fromResources) {
            ClassLoader loader = ConfigsUtil.class.getClassLoader();
            InputStream inputStream = loader.getResourceAsStream(filename);

            Objects.requireNonNull(inputStream, "Can not load resource " + filename);
            return inputStream;
        }
        return new FileInputStream(filename);

    }

    private ConfigsUtil() {

    }
}
