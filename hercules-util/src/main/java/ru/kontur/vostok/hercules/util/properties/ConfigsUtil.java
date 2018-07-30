package ru.kontur.vostok.hercules.util.properties;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Objects;

/**
 * @author Daniil Zhenikhov
 */
public final class ConfigsUtil {
    public static InputStream readConfig(String propertyName, String defaultResource) {
        String filename = System.getProperty(propertyName);
        boolean fromResources = false;
        if (Objects.isNull(filename)) {
            filename = defaultResource;
            fromResources = true;
        }
        try {
            return readProperties(filename, fromResources);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static InputStream readProperties(String filename, boolean fromResources) throws FileNotFoundException {
        if (fromResources) {
            ClassLoader loader = ConfigsUtil.class.getClassLoader();
            return loader.getResourceAsStream(filename);
        }
        return new FileInputStream(filename);

    }

    private ConfigsUtil() {

    }
}
