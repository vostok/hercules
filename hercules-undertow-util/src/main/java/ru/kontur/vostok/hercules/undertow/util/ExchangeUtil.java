package ru.kontur.vostok.hercules.undertow.util;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import ru.kontur.vostok.hercules.http.HttpHeaders;

import java.util.Deque;
import java.util.Optional;

/**
 * @author Gregory Koshelev
 */
@Deprecated
public class ExchangeUtil {
    public static Optional<Integer> extractContentLength(HttpServerExchange exchange) {
        HeaderValues header = exchange.getRequestHeaders().get(HttpHeaders.CONTENT_LENGTH);
        if (header == null || header.isEmpty()) {
            return Optional.empty();
        }

        String value = header.getFirst();
        if (value == null || value.isEmpty()) {
            return Optional.of(-1);
        }

        try {
            return Optional.of(Integer.valueOf(value));
        } catch (NumberFormatException ex) {
            return Optional.of(-1);
        }
    }

    public static Optional<String> extractPathParam(HttpServerExchange exchange, String name) {
        Deque<String> values = exchange.getPathParameters().get(name);
        return getFirstValueIfExists(values);
    }

    public static Optional<String> extractHeaderValue(HttpServerExchange exchange, String name) {
        HeaderValues header = exchange.getRequestHeaders().get(name);
        if (header == null || header.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(header.getFirst());
    }

    public static Optional<String> extractQueryParam(HttpServerExchange exchange, String name) {
        Deque<String> values = exchange.getQueryParameters().get(name);
        return getFirstValueIfExists(values);
    }

    public static Optional<Integer> extractIntegerQueryParam(HttpServerExchange exchange, String name) {
        Optional<String> param = extractQueryParam(exchange, name);
        if (!param.isPresent()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.valueOf(param.get()));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    public static Optional<Long> extractLongQueryParam(HttpServerExchange exchange, String name) {
        Optional<String> param = extractQueryParam(exchange, name);
        if (!param.isPresent()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.valueOf(param.get()));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private static Optional<String> getFirstValueIfExists(Deque<String> values) {
        if (values == null || values.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(values.getFirst());
    }
}
