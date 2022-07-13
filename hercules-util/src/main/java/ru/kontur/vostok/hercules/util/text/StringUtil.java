package ru.kontur.vostok.hercules.util.text;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Gregory Koshelev
 */
public final class StringUtil {
    private StringUtil() {

    }

    public static boolean isNullOrEmpty(CharSequence string) {
        return string == null || string.length() == 0;
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
     * Split source string by delimiter char into non empty substrings.
     *
     * @param source    the source string
     * @param delimiter the delimiter char
     * @return substrings
     * @see StringUtil#split(String, char, boolean)
     */
    public static String[] split(String source, char delimiter) {
        return split(source, delimiter, true);
    }

    /**
     * Split source string by delimiter char into substrings. Empty substrings can be omitted by trim flag.
     *
     * @param source    the source string
     * @param delimiter the delimiter char
     * @param trim      should omit empty substrings
     * @return substrings
     */
    public static String[] split(String source, char delimiter, boolean trim) {
        List<String> substrings = new ArrayList<>();
        int offset = 0;
        int length = source.length();
        int position = 0;
        while (offset < length && (position = source.indexOf(delimiter, offset)) != -1) {
            if (position == offset) {
                if (!trim) {
                    substrings.add("");
                }
                offset++;
            } else {
                substrings.add(source.substring(offset, position));
                offset = position + 1;
            }
        }
        if (offset < length) {
            substrings.add(source.substring(offset));
        } else {
            if (!trim) {
                substrings.add("");
            }
        }
        return substrings.toArray(new String[0]);
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

    public static String emptyToNull(final String original) {
        if (isNullOrEmpty(original)) {
            return null;
        } else {
            return original;
        }
    }

    public static String nullToEmpty(final String original) {
        if (isNullOrEmpty(original)) {
            return "";
        } else {
            return original;
        }
    }

    public static String requireNotEmpty(final String value) {
        if (isNullOrEmpty(value)) {
            throw new IllegalArgumentException("String cannot be an empty string or null");
        }
        return value;
    }

    /**
     * If a {@code value} is not null, returns the {@code value}, otherwise returns {@code defaultValue}.
     *
     * @param value        the source string
     * @param defaultValue the string to be returned, if {@code value} is null
     * @return the {@code value}, if not null, otherwise {@code defaultValue}
     */
    @NotNull
    public static String getOrDefault(@Nullable String value, @NotNull String defaultValue) {
        return value == null ? defaultValue : value;
    }

    /**
     * Partially mask the source string by the mask symbol.
     *
     * @param src        the source string
     * @param maskSymbol the mask symbol
     * @param beginIndex the beginning index, inclusive
     * @return the masked string
     * @throws StringIndexOutOfBoundsException if {@code beginIndex} is negative or larger than the length of the source string
     */
    public static String mask(String src, char maskSymbol, int beginIndex) {
        int length = src.length();
        if (beginIndex < 0 || length <= beginIndex) {
            throw new StringIndexOutOfBoundsException(beginIndex);
        }
        return src.substring(0, beginIndex) + maskSymbol;
    }
}
