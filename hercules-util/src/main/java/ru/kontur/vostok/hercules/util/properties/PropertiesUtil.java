package ru.kontur.vostok.hercules.util.properties;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
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

    public static Properties readProperties(String path) {
        Properties properties = new Properties();
        try(InputStream in = new FileInputStream(path)) {
            properties.load(in);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return properties;
    }

    @SuppressWarnings("unchecked")
    public static <T> Optional<T> getAs(Properties properties, String name, Class<T> clazz) {
        return Optional.ofNullable((T) converters.get(clazz).apply(properties.getProperty(name)));
    }

    public static Supplier<RuntimeException> missingPropertyError(String propertyName) {
        return () -> new RuntimeException(String.format("Missing required property '%s'", propertyName));
    }
}
