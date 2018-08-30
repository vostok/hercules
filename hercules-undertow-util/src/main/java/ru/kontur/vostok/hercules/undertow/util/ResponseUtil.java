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

    public static void forbidden(HttpServerExchange exchange) {
        exchange.setStatusCode(403);
        exchange.endExchange();
    }

    public static void notFound(HttpServerExchange exchange) {
        exchange.setStatusCode(404);
        exchange.endExchange();
    }

    public static void conflict(HttpServerExchange exchange) {
        exchange.setStatusCode(409);
        exchange.endExchange();
    }

    public static void unprocessableEntity(HttpServerExchange exchange) {
        exchange.setStatusCode(422);
        exchange.endExchange();
    }

    public static void internalServerError(HttpServerExchange exchange) {
        exchange.setStatusCode(500);
        exchange.endExchange();
    }

    public static void serviceUnavailable(HttpServerExchange exchange) {
        exchange.setStatusCode(503);
        exchange.endExchange();
    }
}
