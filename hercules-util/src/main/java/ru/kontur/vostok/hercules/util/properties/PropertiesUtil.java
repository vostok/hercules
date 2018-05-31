package ru.kontur.vostok.hercules.util.properties;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class PropertiesUtil {
    public static int get(Properties properties, String name, int defaultValue) {
        String stringValue = properties.getProperty(name);
        if (stringValue == null || stringValue.isEmpty()) {
            return defaultValue;
        }
        return Integer.parseInt(stringValue);
    }

    public static Properties readProperties(String path) {
        Properties properties = new Properties();
        try(InputStream in = new FileInputStream(path)) {
            properties.load(in);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return properties;
    }
}
