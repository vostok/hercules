package ru.kontur.vostok.hercules.util.args;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Util class provides methods to parse strings into appropriate values
 * <br>
 * @author Gregory Koshelev
 *
 * @deprecated Use {@link ru.kontur.vostok.hercules.util.parsing.Parser}
 */
@Deprecated
public class ValueParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValueParser.class);

    /**
     * Try to parse string to int or return the default value otherwise
     * @param stringValue is the string to be parsed
     * @param defaultValue is the default value
     * @return parsed int or the default one otherwise
     *
     * @deprecated Use {@link ru.kontur.vostok.hercules.util.parsing.Parsers#parseInteger(String)}
     */
    @Deprecated
    public static int tryParseInt(String stringValue, int defaultValue) {
        if (stringValue == null || stringValue.isEmpty()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(stringValue);
        } catch (NumberFormatException ex) {
            LOGGER.warn("Wrong int value", ex);

            return defaultValue;
        }
    }

    /**
     * Try to parse string to long or return the default value otherwise
     * @param stringValue is the string to be parsed
     * @param defaultValue is the default value
     * @return parsed long or the default one otherwise
     *
     * @deprecated Use {@link ru.kontur.vostok.hercules.util.parsing.Parsers#parseLong(String)}
     */
    @Deprecated
    public static long tryParseLong(String stringValue, long defaultValue) {
        if (stringValue == null || stringValue.isEmpty()) {
            return defaultValue;
        }

        try {
            return Long.parseLong(stringValue);
        } catch (NumberFormatException ex) {
            LOGGER.warn("Wrong long value", ex);

            return defaultValue;
        }
    }

    /**
     * Try to parse string to boolean or return the default value otherwise
     * @param stringValue is the string to be parsed
     * @param defaultValue is the default value
     * @return parsed boolean or the default one otherwise
     *
     * @deprecated Use {@link ru.kontur.vostok.hercules.util.parsing.Parsers#parseBoolean(String)}
     */
    @Deprecated
    public static boolean tryParseBool(String stringValue, boolean defaultValue) {
        if (stringValue == null || stringValue.isEmpty()) {
            return defaultValue;
        }

        return Boolean.parseBoolean(stringValue);
    }

    /**
     * Try to parse string to enum value of the given type or return the default enum value otherwise
     * @param stringValue is the string to be parsed
     * @param defaultValue is the default value
     * @param enumType is instance of Class for enum type
     * @param <T> is type of enum
     * @return parsed enum value or the default one otherwise
     */
    public static <T extends Enum<T>> T tryParseEnum(String stringValue, T defaultValue, Class<T> enumType) {
        if (stringValue == null || stringValue.isEmpty()) {
            return defaultValue;
        }

        try {
            return Enum.valueOf(enumType, stringValue);
        } catch (IllegalArgumentException exception) {
            LOGGER.warn("Wrong enum value", exception);

            return defaultValue;
        }
    }
}
