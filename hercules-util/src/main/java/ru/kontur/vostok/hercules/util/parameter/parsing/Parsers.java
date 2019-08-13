package ru.kontur.vostok.hercules.util.parameter.parsing;

import ru.kontur.vostok.hercules.util.parameter.ParameterValue;
import ru.kontur.vostok.hercules.util.text.StringUtil;

/**
 * Parsers
 *
 * @author Gregory Koshelev
 */
public final class Parsers {
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

    private Parsers() {
        /* static class */
    }
}
