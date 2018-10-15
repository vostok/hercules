package ru.kontur.vostok.hercules.util.parsing;

import ru.kontur.vostok.hercules.util.functional.Result;

/**
 * Parsers
 *
 * @author Kirill Sulim
 */
public final class Parsers {

    public static Result<String, String> parseString(String s) {
        return Result.ok(s);
    }

    public static Result<Integer, String> parseInteger(String s) {
        try {
            return Result.ok(Integer.parseInt(s));
        }
        catch (NumberFormatException e) {
            return Result.error(String.format("Invalid integer '%s'", s));
        }
    }

    public static Result<Long, String> parseLong(String s) {
        try {
            return Result.ok(Long.parseLong(s));
        }
        catch (NumberFormatException e) {
            return Result.error(String.format("Invalid long '%s'", s));
        }
    }

    public static Result<Boolean, String> parseBoolean(String s) {
        if ("true".equals(s.toLowerCase())) {
            return Result.ok(true);
        }

        if ("false".equals(s.toLowerCase())) {
            return Result.ok(false);
        }

        return Result.error(String.format("Invalid boolean '%s'", s));
    }

    private Parsers() {
    }
}
