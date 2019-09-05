package ru.kontur.vostok.hercules.util.parameter.parsing;

import ru.kontur.vostok.hercules.util.parameter.ParameterValue;
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
                return ParameterValue.empty();
            }
            String string = s.trim().toLowerCase();
            if ("true".equals(string)) {
                return ParameterValue.of(true);
            }
            if ("false".equals(string)) {
                return ParameterValue.of(false);
            }
            return ParameterValue.invalid("Cannot parse string to boolean");
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
                return ParameterValue.empty();
            }
            try {
                return ParameterValue.of(Short.valueOf(s));
            } catch (NumberFormatException ex) {
                return ParameterValue.invalid(ex.getMessage());
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
                return ParameterValue.empty();
            }
            try {
                return ParameterValue.of(Integer.valueOf(s));
            } catch (NumberFormatException ex) {
                return ParameterValue.invalid(ex.getMessage());
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
                return ParameterValue.empty();
            }
            try {
                return ParameterValue.of(Long.valueOf(s));
            } catch (NumberFormatException ex) {
                return ParameterValue.invalid(ex.getMessage());
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
                return ParameterValue.empty();
            }
            return ParameterValue.of(s);
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
                return ParameterValue.empty();
            }
            return ParameterValue.of(s.split(ELEMENT_DELIMITER));
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
                return ParameterValue.empty();
            }
            try {
                T result = function.apply(s);
                return result != null ? ParameterValue.of(result) : ParameterValue.empty();
            } catch (Exception ex) {
                return ParameterValue.invalid(ex.getMessage());
            }
        };
    }

    private Parsers() {
        /* static class */
    }
}
