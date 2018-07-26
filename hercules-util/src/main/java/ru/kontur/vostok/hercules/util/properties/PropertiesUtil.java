package ru.kontur.vostok.hercules.util.properties;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Gregory Koshelev
 */
public class PropertiesUtil {

    private static Map<Class<?>, Function<String, ?>> converters = new HashMap<>();
    static {
        converters.put(String.class, Function.identity());
        converters.put(Integer.class, s -> {
            try {
                return Integer.valueOf(s);
            } catch (NumberFormatException e) {
                return null;
            }
        });
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
        Set<String> set = new HashSet<>(split.length);
        set.addAll(Arrays.asList(split));
        return set;
    }

    public static Properties readProperties(String path) {
        Properties properties = new Properties();
        try(InputStream in = new FileInputStream(path)) {
            properties.load(in);
        } catch (IOException ex) {
            // TODO: log
            ex.printStackTrace();
        }
        return properties;
    }

    @SuppressWarnings("unchecked")
    public static <T> Optional<T> getAs(Properties properties, String name, Class<T> clazz) {
        Function<String, ?> converter = converters.get(clazz);
        if (Objects.isNull(converter)) {
            throw new RuntimeException(String.format("No converter found for class %s", clazz));
        }
        return Optional.ofNullable((T) converter.apply(properties.getProperty(name)));
    }

    public static Supplier<RuntimeException> missingPropertyError(String propertyName) {
        return () -> new RuntimeException(String.format("Missing required property '%s'", propertyName));
    }

    public static Properties subProperties(Properties properties, String prefix) {
        return subProperties(properties, prefix, '.');
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
