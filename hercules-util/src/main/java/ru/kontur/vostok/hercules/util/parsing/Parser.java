package ru.kontur.vostok.hercules.util.parsing;

import ru.kontur.vostok.hercules.util.functional.Result;

/**
 * Parser - common interface for parser
 *
 * @author Kirill Sulim
 */
@FunctionalInterface
public interface Parser<T> {

    /**
     * Parse value from string
     *
     * @param s string to parse
     * @return Result of parsed value or string with description of parsing error
     */
    Result<T, String> parse(String s);
}
