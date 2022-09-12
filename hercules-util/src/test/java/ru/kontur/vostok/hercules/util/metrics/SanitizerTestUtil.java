package ru.kontur.vostok.hercules.util.metrics;

import ru.kontur.vostok.hercules.util.throwable.NotImplementedException;

import java.util.stream.IntStream;

/**
 * Utils for sanitizers implementation tests.
 *
 * @author Aleksandr Yuferov
 */
class SanitizerTestUtil {

    static String generateStringByRangeClosed(char begin, char end) {
        return IntStream.rangeClosed(begin, end)
                .collect(
                        () -> new StringBuilder(end - begin),
                        (acc, curr) -> acc.append((char) curr),
                        (lhs, rhs) -> {
                            throw new NotImplementedException();
                        }
                )
                .toString();
    }
}
