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

    /**
     * Split source string by delimiter char into substrings
     *
     * @param source    string
     * @param delimiter char
     * @param parts     is result count of substrings
     * @return substrings
     * @throws IllegalArgumentException if count of delimiter chars is lesser than parts - 1
     */
    public static String[] split(String source, char delimiter, int parts) {
        String[] substrings = new String[parts];
        int index = 0;
        int offset = 0;
        int length = source.length();
        while (index < parts - 1) {
            int position = source.indexOf(delimiter, offset);
            if (position == -1 || position == length - 1) {
                throw new IllegalArgumentException("Cannot split string into parts");
            }
            substrings[index++] = source.substring(offset, position);
            offset = position + 1;
        }
        substrings[parts - 1] = source.substring(offset);
        return substrings;
    }

    /**
     * Repeat string multiple times
     *
     * @param s     is repeated string
     * @param count is repetition count
     * @return resulting string with repetition
     */
    public static String repeat(final String s, int count) {
        return new String(new char[count]).replace("\0", s);
    }

    /**
     * Repeat char multiple times
     *
     * @param c     is repeated char
     * @param count is repetition count
     * @return resulting string with repetition
     */
    public static String repeat(char c, int count) {
        char[] result = new char[count];
        Arrays.fill(result, c);
        return new String(result);
    }
}
