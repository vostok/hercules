package ru.kontur.vostok.hercules.util.text;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Gregory Koshelev
 */
public final class StringUtil {
    private StringUtil() {

    }

    public static boolean isNullOrEmpty(String string) {
        return string == null || string.isEmpty();
    }

    public static boolean tryParseBoolean(String string, boolean defaultValue) {
        return isNullOrEmpty(string) ? defaultValue : Boolean.parseBoolean(string);
    }

    public static short tryParseShort(String string, short defaultValue) {
        if (isNullOrEmpty(string)) {
            return defaultValue;
        }
        try {
            return Short.parseShort(string);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static List<String> toList(String string, char delimiter) {
        if (isNullOrEmpty(string)) {
            return Collections.emptyList();
        }
        String[] values = string.split(String.valueOf(delimiter));
        return Arrays.asList(values);
    }
}
