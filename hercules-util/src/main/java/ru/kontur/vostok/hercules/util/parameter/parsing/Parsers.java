package ru.kontur.vostok.hercules.util.parameter.parsing;

import ru.kontur.vostok.hercules.util.text.StringUtil;

import java.util.function.Function;

/**
 * Parsers
 *
 * @author Gregory Koshelev
 */
public final class Parsers {
    private static String ELEMENT_DELIMITER = ",";

    /**
     * Boolean parser.
     * <p>
     * Parse string "true" to {@link Boolean#TRUE}, string "false" to {@link Boolean#FALSE}.
     * Other values are treated as invalid.
     * Parsing is case insensitive.
     *
     * @return boolean parser
     */
    public static Parser<Boolean> forBoolean() {
        return s -> {
            if (StringUtil.isNullOrEmpty(s)) {
                return ParsingResult.empty();
            }
            String string = s.trim().toLowerCase();
            if ("true".equals(string)) {
                return ParsingResult.of(true);
            }
            if ("false".equals(string)) {
                return ParsingResult.of(false);
            }
            return ParsingResult.invalid("Cannot parse string '" + s + "' to boolean");
        };
    }

    /**
     * Short parser.
     *
     * @return short parser
     */
    public static Parser<Short> forShort() {
        return s -> {
            if (StringUtil.isNullOrEmpty(s)) {
                return ParsingResult.empty();
            }
            try {
                return ParsingResult.of(Short.valueOf(s));
            } catch (NumberFormatException ex) {
                return ParsingResult.invalid(ex.getMessage());
            }
        };
    }

    /**
     * Integer parser.
     *
     * @return integer parser
     */
    public static Parser<Integer> forInteger() {
        return s -> {
            if (StringUtil.isNullOrEmpty(s)) {
                return ParsingResult.empty();
            }
            try {
                return ParsingResult.of(Integer.valueOf(s));
            } catch (NumberFormatException ex) {
                return ParsingResult.invalid(ex.getMessage());
            }
        };
    }

    /**
     * Long parser.
     *
     * @return long parser
     */
    public static Parser<Long> forLong() {
        return s -> {
            if (StringUtil.isNullOrEmpty(s)) {
                return ParsingResult.empty();
            }
            try {
                return ParsingResult.of(Long.valueOf(s));
            } catch (NumberFormatException ex) {
                return ParsingResult.invalid(ex.getMessage());
            }
        };
    }

    /**
     * String parser.
     *
     * @return string parser
     */
    public static Parser<String> forString() {
        return s -> {
            if (s == null) {
                return ParsingResult.empty();
            }
            return ParsingResult.of(s);
        };
    }

    /**
     * String array parser.
     *
     * @return string array parser
     */
    public static Parser<String[]> forStringArray() {
        return s -> {
            if (StringUtil.isNullOrEmpty(s)) {
                return ParsingResult.empty();
            }
            return ParsingResult.of(s.split(ELEMENT_DELIMITER));
        };
    }

    /**
     * Enum parser.
     * <p>
     * Parser is case-sensitive as it uses {@link Enum#valueOf(Class, String)}
     *
     * @param enumType the enum type class
     * @param <T> the enum type
     * @return enum parser
     */
    public static <T extends Enum<T>> Parser<T> forEnum(Class<T> enumType) {
        return s -> {
            if (StringUtil.isNullOrEmpty(s)) {
                return ParsingResult.empty();
            }
            try {
                return ParsingResult.of(Enum.valueOf(enumType, s));
            } catch (IllegalArgumentException ex) {
                return ParsingResult.invalid(ex.getMessage());
            }
        };
    }

    /**
     * Object parser uses provided function to parse value from string.
     *
     * @param function the parsing function
     * @param <T>      the object type
     * @return object parser
     */
    public static <T> Parser<T> fromFunction(Function<String, T> function) {
        return s -> {
            if (StringUtil.isNullOrEmpty(s)) {
                return ParsingResult.empty();
            }
            try {
                T result = function.apply(s);
                return result != null ? ParsingResult.of(result) : ParsingResult.empty();
            } catch (Exception ex) {
                return ParsingResult.invalid(ex.getMessage());
            }
        };
    }

    private Parsers() {
        /* static class */
    }
}
