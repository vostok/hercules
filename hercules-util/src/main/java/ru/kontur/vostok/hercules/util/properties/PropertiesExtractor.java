package ru.kontur.vostok.hercules.util.properties;

import ru.kontur.vostok.hercules.util.text.StringUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
public class PropertiesExtractor {
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

    public static short getShort(Properties properties, String name, short defaultValue) {
        String stringValue = properties.getProperty(name);
        return StringUtil.tryParseShort(stringValue, defaultValue);
    }

    public static boolean getBoolean(Properties properties, String name, boolean defaultValue) {
        String stringValue = properties.getProperty(name);
        return StringUtil.tryParseBoolean(stringValue, defaultValue);
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
        return new HashSet<>(toList(properties, name));
    }

    public static List<String> toList(Properties properties, String name) {
        String value = properties.getProperty(name);
        return StringUtil.toList(value, ',');
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

    public static <T> T getRequiredProperty(Properties properties, String name, Class<T> clazz) {
        return getAs(properties, name, clazz).orElseThrow(missingPropertyError(name));
    }
}
