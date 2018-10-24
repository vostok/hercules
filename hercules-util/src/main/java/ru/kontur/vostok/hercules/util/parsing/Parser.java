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
     * Parse string
     *
     * @param s string to parse
     * @return
     */
    Result<T, String> parse(String s);
}
