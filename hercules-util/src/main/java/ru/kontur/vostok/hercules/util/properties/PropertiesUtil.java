package ru.kontur.vostok.hercules.util.properties;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * @author Gregory Koshelev
 */
public class PropertiesUtil {
    public static short get(Properties properties, String name, short defaultValue) {
        String stringValue = properties.getProperty(name);
        if (stringValue == null || stringValue.isEmpty()) {
            return defaultValue;
        }
        return Short.parseShort(stringValue);
    }

    public static int get(Properties properties, String name, int defaultValue) {
        String stringValue = properties.getProperty(name);
        if (stringValue == null || stringValue.isEmpty()) {
            return defaultValue;
        }
        return Integer.parseInt(stringValue);
    }

    public static long get(Properties properties, String name, long defaultValue) {
        String stringValue = properties.getProperty(name);
        if (stringValue == null || stringValue.isEmpty()) {
            return defaultValue;
        }
        return Long.parseLong(stringValue);
    }

    public static Set<String> toSet(Properties properties, String name) {
        String value = properties.getProperty(name, "");
        String[] split = value.split(",");
        Set<String> set = new HashSet<String>(split.length);
        set.addAll(Arrays.asList(split));
        return set;
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

    public static Properties subProperties(Properties properties, String prefix, char delimiter) {
        Properties props = new Properties();
        int prefixLength = prefix.length();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String name = ((String)entry.getKey());
            if (name.length() > prefixLength && name.startsWith(prefix) && name.charAt(prefixLength) == delimiter) {
                props.setProperty(name.substring(prefixLength + 1), (String) entry.getValue());
            }
        }
        return props;
    }
}
