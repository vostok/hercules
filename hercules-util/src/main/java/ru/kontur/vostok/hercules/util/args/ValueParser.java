package ru.kontur.vostok.hercules.util.args;

/**
 * Util class provides methods to parse strings into appropriate values
 * <br>
 * @author Gregory Koshelev
 */
public class ValueParser {
    /**
     * Try to parse string to int or return the default value otherwise
     * @param stringValue is the string to be parsed
     * @param defaultValue is the default value
     * @return parsed int or the default one otherwise
     */
    public static int tryParseInt(String stringValue, int defaultValue) {
        if (stringValue == null || stringValue.isEmpty()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(stringValue);
        } catch (NumberFormatException ex) {
            ex.printStackTrace();

            return defaultValue;
        }
    }

    /**
     * Try to parse string to long or return the default value otherwise
     * @param stringValue is the string to be parsed
     * @param defaultValue is the default value
     * @return parsed long or the default one otherwise
     */
    public static long tryParseLong(String stringValue, long defaultValue) {
        if (stringValue == null || stringValue.isEmpty()) {
            return defaultValue;
        }

        try {
            return Long.parseLong(stringValue);
        } catch (NumberFormatException ex) {
            ex.printStackTrace();

            return defaultValue;
        }
    }

    /**
     * Try to parse string to boolean or return the default value otherwise
     * @param stringValue is the string to be parsed
     * @param defaultValue is the default value
     * @return parsed boolean or the default one otherwise
     */
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
            exception.printStackTrace();

            return defaultValue;
        }
    }
}
