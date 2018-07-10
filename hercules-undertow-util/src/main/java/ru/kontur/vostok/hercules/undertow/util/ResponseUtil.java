package ru.kontur.vostok.hercules.undertow.util;

import io.undertow.server.HttpServerExchange;

/**
 * @author Gregory Koshelev
 */
public class ResponseUtil {
    public static void ok(HttpServerExchange exchange) {
        exchange.setStatusCode(200);
        exchange.endExchange();
    }

    public static void badRequest(HttpServerExchange exchange) {
        exchange.setStatusCode(400);
        exchange.endExchange();
    }

    public static void unauthorized(HttpServerExchange exchange) {
        exchange.setStatusCode(401);
        exchange.endExchange();
    }

    public static void internalServerError(HttpServerExchange exchange) {
        exchange.setStatusCode(500);
        exchange.endExchange();
    }
}
