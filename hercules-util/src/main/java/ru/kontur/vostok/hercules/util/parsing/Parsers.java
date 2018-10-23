package ru.kontur.vostok.hercules.util.parsing;

import ru.kontur.vostok.hercules.util.functional.Result;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Parsers
 *
 * @author Kirill Sulim
 */
public final class Parsers {

    public static Result<String, String> parseString(String s) {
        return Result.ok(s);
    }

    public static Result<Short, String> parseShort(String s) {
        s = s.trim();
        try {
            return Result.ok(Short.parseShort(s));
        }
        catch (NumberFormatException e) {
            return Result.error(String.format("Invalid short '%s'", s));
        }
    }

    public static Result<Integer, String> parseInteger(String s) {
        s = s.trim();
        try {
            return Result.ok(Integer.parseInt(s));
        }
        catch (NumberFormatException e) {
            return Result.error(String.format("Invalid integer '%s'", s));
        }
    }

    public static Result<Long, String> parseLong(String s) {
        s = s.trim();
        try {
            return Result.ok(Long.parseLong(s));
        }
        catch (NumberFormatException e) {
            return Result.error(String.format("Invalid long '%s'", s));
        }
    }

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

    public static <T> Parser<List<T>> parseList(Parser<T> parser) {
        return parseList(parser, ",");
    }

    public static <T> Parser<List<T>> parseList(Parser<T> parser, String regexp) {
        return s -> {
            String[] split = s.split(regexp);
            List<T> values = new ArrayList<>(split.length);

            for (int i = 0; i < split.length; ++i) {
                Result<T, String> parsed = parser.parse(split[i]);
                if (parsed.isOk()) {
                    values.add(parsed.get());
                }
                else {
                    return Result.error(String.format("Error at index '%d': %s", i, parsed.getError()));
                }
            }

            return Result.ok(values);
        };
    }

    public static <T> Parser<T[]> parseArray(Class<T> clazz, Parser<T> parser) {
        return parseArray(clazz, parser, ",");
    }

    public static <T> Parser<T[]> parseArray(Class<T> clazz, Parser<T> parser, String regexp) {
        return s -> {
            String[] split = s.split(regexp);

            @SuppressWarnings("unchecked")
            T[] values = (T[]) Array.newInstance(clazz, split.length);

            for (int i = 0; i < split.length; ++i) {
                Result<T, String> parsed = parser.parse(split[i]);
                if (parsed.isOk()) {
                    values[i] = parsed.get();
                }
                else {
                    return Result.error(String.format("Error at index '%d': %s", i, parsed.getError()));
                }
            }

            return Result.ok(values);
        };
    }

    private Parsers() {
    }
}
