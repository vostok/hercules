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
 *
 * @deprecated Use {@link PropertyDescriptions}
 */
@Deprecated
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
        converters.put(Boolean.class, s -> {
            if (Objects.isNull(s)) {
                return null;
            }
            switch (s.toLowerCase()) {
                case "true": return Boolean.TRUE;
                case "false": return Boolean.FALSE;
                default: return null;
            }
        });
    }

    /**
     * @deprecated Use {@link PropertyDescriptions#shortProperty(String)}
     */
    @Deprecated
    public static short getShort(Properties properties, String name, short defaultValue) {
        String stringValue = properties.getProperty(name);
        return StringUtil.tryParseShort(stringValue, defaultValue);
    }

    /**
     * @deprecated Use {@link PropertyDescriptions#booleanProperty(String)}
     */
    @Deprecated
    public static boolean getBoolean(Properties properties, String name, boolean defaultValue) {
        String stringValue = properties.getProperty(name);
        return StringUtil.tryParseBoolean(stringValue, defaultValue);
    }

    /**
     * @deprecated Use {@link PropertyDescriptions#integerProperty(String)}
     */
    @Deprecated
    public static int get(Properties properties, String name, int defaultValue) {
        String stringValue = properties.getProperty(name);
        if (stringValue == null || stringValue.isEmpty()) {
            return defaultValue;
        }
        return Integer.parseInt(stringValue);
    }

    /**
     * @deprecated Use {@link PropertyDescriptions#longProperty(String)}
     */
    @Deprecated
    public static long get(Properties properties, String name, long defaultValue) {
        String stringValue = properties.getProperty(name);
        if (stringValue == null || stringValue.isEmpty()) {
            return defaultValue;
        }
        return Long.parseLong(stringValue);
    }

    /**
     * @deprecated Use {@link PropertyDescriptions#setOfStringsProperty(String)}
     */
    @Deprecated
    public static Set<String> toSet(Properties properties, String name) {
        return new HashSet<>(toList(properties, name));
    }

    /**
     * @deprecated Use {@link PropertyDescriptions#listOfStringsProperty(String)}
     */
    @Deprecated
    public static List<String> toList(Properties properties, String name) {
        String value = properties.getProperty(name);
        return StringUtil.toList(value, ',');
    }

    /**
     * @deprecated Use {@link PropertyDescriptions#propertyOfType(Class, String)}
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public static <T> Optional<T> getAs(Properties properties, String name, Class<T> clazz) {
        Function<String, ?> converter = converters.get(clazz);
        if (Objects.isNull(converter)) {
            throw new RuntimeException(String.format("No converter found for class %s", clazz));
        }
        return Optional.ofNullable((T) converter.apply(properties.getProperty(name)));
    }

    /**
     * @deprecated Use {@link PropertyDescription} inner required property check
     */
    @Deprecated
    public static Supplier<RuntimeException> missingPropertyError(String propertyName) {
        return () -> new RuntimeException(String.format("Missing required property '%s'", propertyName));
    }

    /**
     * @deprecated Use {@link PropertyDescription} inner required property check
     */
    @Deprecated
    public static <T> T getRequiredProperty(Properties properties, String name, Class<T> clazz) {
        return getAs(properties, name, clazz).orElseThrow(missingPropertyError(name));
    }
}
