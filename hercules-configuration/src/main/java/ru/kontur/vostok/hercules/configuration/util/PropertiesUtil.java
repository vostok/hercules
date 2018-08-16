package ru.kontur.vostok.hercules.configuration.util;

import java.util.Map;
import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class PropertiesUtil {
    public static Properties ofScope(Properties properties, String scope) {
        return subProperties(properties, scope, '.');
    }

    public static Properties subProperties(Properties properties, String prefix, char delimiter) {
        Properties props = new Properties();
        int prefixLength = prefix.length();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String name = ((String) entry.getKey());
            if (name.length() > prefixLength && name.startsWith(prefix) && name.charAt(prefixLength) == delimiter) {
                props.setProperty(name.substring(prefixLength + 1), (String) entry.getValue());
            }
        }
        return props;
    }

}
