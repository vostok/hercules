package ru.kontur.vostok.hercules.http.handler;

import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;

/**
 * @author Gregory Koshelev
 */
public class PingHandler implements HttpHandler {
    @Override
    public void handle(HttpServerRequest request) {
        request.complete(HttpStatusCodes.OK);
    }
}
