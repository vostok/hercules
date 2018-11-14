package ru.kontur.vostok.hercules.undertow.util.handlers;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

/**
 * PingHandler - standard ping handler, set exchange status to 200 OK.
 *
 * @author Kirill Sulim
 */
public class PingHandler implements HttpHandler {

    public static final PingHandler INSTANCE = new PingHandler();

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        exchange.setStatusCode(200);
        exchange.endExchange();
    }
}
