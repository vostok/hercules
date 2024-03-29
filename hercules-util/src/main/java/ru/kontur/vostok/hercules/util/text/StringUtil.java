package ru.kontur.vostok.hercules.util.text;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.IntPredicate;

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

    /**
     * Replace all incorrect characters with placeholder ({@code _}).
     *
     * @param source          Source string.
     * @param isCorrectSymbol Predicates that checks each symbol correctness.
     * @return Sanitized string.
     */
    public static String sanitize(String source, IntPredicate isCorrectSymbol) {
        int len = source.length();
        int firstIncorrect = -1;
        for (int i = 0; i < len; i++) {
            if (!isCorrectSymbol.test(source.charAt(i))) {
                firstIncorrect = i;
                break;
            }
        }
        if (firstIncorrect == -1) {
            return source;
        }
        var builder = new StringBuilder(len);
        builder.append(source, 0, firstIncorrect);
        builder.append('_');
        int prevIndex = firstIncorrect + 1;
        for (int i = prevIndex; i < len; i++) {
            if (isCorrectSymbol.test(source.charAt(i))) {
                continue;
            }
            builder.append(source, prevIndex, i);
            builder.append('_');
            prevIndex = i + 1;
        }
        builder.append(source, prevIndex, source.length());
        return builder.toString();
    }

    private static final int ESTIMATE_COUNT_OF_SPACES = 5;

    /**
     * Changes style of naming from camelCase to snake_case.
     *
     * @param name Some name in camelCase.
     * @return The same name in snake_case style.
     */
    public static String camelToSnake(CharSequence name) {
        StringBuilder result = new StringBuilder(name.length() + ESTIMATE_COUNT_OF_SPACES);
        result.append(Character.toLowerCase(name.charAt(0)));
        for (int i = 1; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (Character.isUpperCase(ch)) {
                result.append('_');
                result.append(Character.toLowerCase(ch));
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    /**
     * Compares {@code CharSequence}s, ignoring case considerations (null safe impl).
     *
     * @param lhs First string.
     * @param rhs Second string.
     * @return Result of comparing.
     */
    public static boolean equalsIgnoreCase(CharSequence lhs, CharSequence rhs) {
        if (lhs == rhs) {
            return true;
        }
        if (lhs == null || rhs == null) {
            return false;
        }
        int length = lhs.length();
        if (rhs.length() != length) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (Character.toLowerCase(lhs.charAt(i)) != Character.toLowerCase(rhs.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
