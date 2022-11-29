package ru.kontur.vostok.hercules.sentry.client.impl.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.sentry.client.SentryConverterUtil;
import ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model.JsonUnknown;
import ru.kontur.vostok.hercules.util.text.StringUtil;

/**
 * Converts values from Hercules event to Sentry contexts (app, device etc.)
 *
 * @author Aleksandr Yuferov
 */
public class ContextConverter<T extends JsonUnknown> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContextConverter.class);
    private static final String UNKNOWN_FIELD = "unknown";

    private final Map<String, Field> fields;
    private final Constructor<T> constructor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Constructor.
     * <p/>
     * Analog of {@link ContextConverter#ContextConverter(Class, Set)} with empty set as {@code excludedFields} parameter.
     *
     * @param type Class of Sentry context DTO.
     * @see ContextConverter#ContextConverter(Class, Set)
     */
    public ContextConverter(Class<T> type) {
        this(type, Set.of());
    }

    /**
     * Constructor.
     *
     * @param type           Class of Sentry context DTO.
     * @param excludedFields Set of excluded fields names.
     * @throws IllegalStateException if no default constructor is defined in type.
     */
    public ContextConverter(Class<T> type, Set<String> excludedFields) {
        this.constructor = extractConstructor(type);
        this.fields = Arrays.stream(type.getDeclaredFields())
                .filter(field -> !excludedFields.contains(field.getName()))
                .filter(field -> !UNKNOWN_FIELD.equals(field.getName()))
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .filter(field -> !Modifier.isFinal(field.getModifiers()))
                .peek(field -> field.setAccessible(true))
                .collect(Collectors.toUnmodifiableMap(field -> StringUtil.camelToSnake(field.getName()),
                        Function.identity()));
    }

    /**
     * Convert data to Sentry context DTO.
     *
     * @param data Data to convert.
     * @return Fully initialized Sentry context DTO.
     */
    public T convert(Map<? extends CharSequence, Variant> data) {
        T object = createInstance();
        Map<String, Object> unknown = new HashMap<>();
        for (Map.Entry<? extends CharSequence, Variant> pair : data.entrySet()) {
            String key = pair.getKey().toString();
            Field field = fields.get(key);
            Object extractedValue = SentryConverterUtil.extractObject(pair.getValue());
            if (field == null) {
                unknown.put(key, extractedValue);
                continue;
            }
            Object convertedValue;
            if (!field.getType().isAssignableFrom(extractedValue.getClass())) {
                convertedValue = tryConvertValue(extractedValue, field.getType());
                if (convertedValue == null) {
                    unknown.put(key, extractedValue);
                    continue;
                }
            } else {
                convertedValue = extractedValue;
            }
            setValue(object, field, convertedValue);
        }
        object.setUnknown(unknown);
        return object;
    }

    private Constructor<T> extractConstructor(Class<T> type) {
        try {
            return type.getConstructor();
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private T createInstance() {
        try {
            return constructor.newInstance();
        } catch (Exception exception) {
            throw new IllegalStateException("cannot construct object: " + exception.getMessage(), exception);
        }
    }

    private Object tryConvertValue(Object fromValue, Class<?> toValueType) {
        try {
            return objectMapper.convertValue(fromValue, toValueType);
        } catch (Exception exception) {
            LOGGER.debug("cannot convert value from {} to {}: {}", fromValue.getClass(), toValueType, exception.getMessage());
            return null;
        }
    }

    private void setValue(T object, Field field, Object convertedValue) {
        try {
            field.set(object, convertedValue);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
