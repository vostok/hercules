package ru.kontur.vostok.hercules.undertow.util;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.nio.charset.StandardCharsets;

/**
 * @author Gregory Koshelev
 */
public class ResponseUtil {

    public static void ok(HttpServerExchange exchange) {
        exchange.setStatusCode(200);
        exchange.endExchange();
    }

    public static void okJson(HttpServerExchange exchange, String jsonString) {
        exchange.setStatusCode(200);
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, ContentTypes.APPLICATION_JSON);
        exchange.getResponseSender().send(jsonString, StandardCharsets.UTF_8);
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

    public static void requestEntityTooLarge(HttpServerExchange exchange) {
        exchange.setStatusCode(413);
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
