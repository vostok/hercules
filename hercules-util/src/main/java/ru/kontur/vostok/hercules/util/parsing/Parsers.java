package ru.kontur.vostok.hercules.util.parsing;

import ru.kontur.vostok.hercules.util.functional.Result;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntFunction;

/**
 * Parsers - collection of common parsers
 *
 * @author Kirill Sulim
 * @deprecated use {@link ru.kontur.vostok.hercules.util.parameter.parsing.Parsers} instead
 */
@Deprecated
public final class Parsers {

    private static final String DEFAULT_SEPARATOR = ",";

    /**
     * Parse string
     *
     * @return original string
     */
    public static Result<String, String> parseString(String s) {
        return Result.ok(s);
    }

    /**
     * Parse short
     *
     * @param s string
     * @return parsed short or error description
     */
    public static Result<Short, String> parseShort(String s) {
        s = s.trim();
        try {
            return Result.ok(Short.parseShort(s));
        } catch (NumberFormatException e) {
            return Result.error(String.format("Invalid short '%s'", s));
        }
    }

    /**
     * Parse integer
     *
     * @param s string
     * @return parsed integer or error description
     */
    public static Result<Integer, String> parseInteger(String s) {
        s = s.trim();
        try {
            return Result.ok(Integer.parseInt(s));
        } catch (NumberFormatException e) {
            return Result.error(String.format("Invalid integer '%s'", s));
        }
    }

    /**
     * Parse long
     *
     * @param s string
     * @return parsed long or error description
     */
    public static Result<Long, String> parseLong(String s) {
        s = s.trim();
        try {
            return Result.ok(Long.parseLong(s));
        } catch (NumberFormatException e) {
            return Result.error(String.format("Invalid long '%s'", s));
        }
    }

    /**
     * Parse boolean
     *
     * @param s string
     * @return parsed boolean or error description
     */
    public static Result<Boolean, String> parseBoolean(String s) {
        s = s.trim();
        if ("true".equals(s.toLowerCase())) {
            return Result.ok(true);
        }

        if ("false".equals(s.toLowerCase())) {
            return Result.ok(false);
        }

        return Result.error(String.format("Invalid boolean '%s'", s));
    }

    /**
     * Parse UUID
     *
     * @param s string
     * @return parsed UUID or error description
     */
    public static Result<UUID, String> parseUuid(String s) {
        s = s.trim();
        try {
            return Result.ok(UUID.fromString(s));
        } catch (IllegalArgumentException e) {
            return Result.error(String.format("Invalid UUID '%s'", s));
        }
    }

    /**
     * Parse string as list of strings separated by {@code DEFAULT_SEPARATOR} and then parse each of strings with
     * {@code parser}
     *
     * @param parser single value parser
     * @return parser of list of values
     */
    public static <T> Parser<List<T>> parseList(Parser<T> parser) {
        return parseList(parser, DEFAULT_SEPARATOR);
    }

    /**
     * Parse string as list of strings separated by {@code regexp} and then parse each of strings with {@code parser}
     *
     * @param parser single value parser
     * @param regexp string separator
     * @return parser of list of values
     */
    public static <T> Parser<List<T>> parseList(Parser<T> parser, String regexp) {
        return parseCollection(parser, ArrayList::new, regexp);
    }

    /**
     * Parse string as set of strings separated by {@code DEFAULT_SEPARATOR} and then parse each of strings with
     * {@code parser}
     *
     * @param parser single value parser
     * @return parser of set of values
     */
    public static <T> Parser<Set<T>> parseSet(Parser<T> parser) {
        return parseSet(parser, DEFAULT_SEPARATOR);
    }

    /**
     * Parse string as set of strings separated by {@code regexp} and then parse each of strings with {@code parser}
     *
     * @param parser single value parser
     * @param regexp string separator
     * @return parser of set of values
     */
    public static <T> Parser<Set<T>> parseSet(Parser<T> parser, String regexp) {
        return parseCollection(parser, HashSet::new, regexp);
    }

    /**
     * Parse string as collection of strings of type {@code C} and then parse each of strings with {@code parser}
     *
     * @param parser single value parser
     * @param collectionSupplier collection constructor accepting collection size
     * @param regexp string separator
     * @param <T> value type
     * @param <C> collection type
     * @return parser of collection of type {@code C} of values
     */
    public static <T, C extends Collection<T>> Parser<C> parseCollection(Parser<T> parser, IntFunction<? extends C> collectionSupplier, String regexp) {
        return s -> {
            String[] split = s.split(regexp);
            C values = collectionSupplier.apply(split.length);

            for (int i = 0; i < split.length; ++i) {
                Result<T, String> parsed = parser.parse(split[i]);
                if (parsed.isOk()) {
                    values.add(parsed.get());
                } else {
                    return Result.error(String.format("Error at index '%d': %s", i, parsed.getError()));
                }
            }

            return Result.ok(values);
        };
    }

    /**
     * Parse string as array of strings separated by {@code DEFAULT_SEPARATOR} and then parse each of strings with
     * {@code parser}
     *
     * @param parser single value parser
     * @return parser of array of values
     */
    public static <T> Parser<T[]> parseArray(Class<T> clazz, Parser<T> parser) {
        return parseArray(clazz, parser, DEFAULT_SEPARATOR);
    }

    /**
     * Parse string as array of strings separated by {@code regexp} and then parse each of strings with {@code parser}
     *
     * @param parser single value parser
     * @param regexp string separator
     * @return parser of array of values
     */
    public static <T> Parser<T[]> parseArray(Class<T> clazz, Parser<T> parser, String regexp) {
        return s -> {
            String[] split = s.split(regexp);

            @SuppressWarnings("unchecked")
            T[] values = (T[]) Array.newInstance(clazz, split.length);

            for (int i = 0; i < split.length; ++i) {
                Result<T, String> parsed = parser.parse(split[i]);
                if (parsed.isOk()) {
                    values[i] = parsed.get();
                } else {
                    return Result.error(String.format("Error at index '%d': %s", i, parsed.getError()));
                }
            }

            return Result.ok(values);
        };
    }

    /**
     * Use {@code defaultValue} if string is null or enmpty
     *
     * @param parser value parser
     * @param defaultValue default value
     * @return Parser with default value
     */
    public static <T> Parser<T> withDefaultValue(Parser<T> parser, T defaultValue) {
        return s -> {
            if (Objects.isNull(s) || s.isEmpty()) {
                return Result.ok(defaultValue);
            } else {
                return parser.parse(s);
            }
        };
    }

    /**
     * Create parser of enum pf type {@code clazz}
     *
     * @param clazz type of enum
     * @return enum parser
     */
    public static <T extends Enum<T>> Parser<T> enumParser(Class<T> clazz) {
        return s -> {
            try {
                return Result.ok(Enum.valueOf(clazz, s.trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                return Result.error(String.format("Cannot parse '%s' as enum of class %s", s, clazz.getCanonicalName()));
            }
        };
    }

    private Parsers() {
        /* static class */
    }
}
