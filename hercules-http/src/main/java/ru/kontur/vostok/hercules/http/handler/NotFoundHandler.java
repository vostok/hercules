package ru.kontur.vostok.hercules.http.handler;

import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;

/**
 * @author Gregory Koshelev
 */
public final class NotFoundHandler implements HttpHandler {
    @Override
    public void handle(HttpServerRequest request) {
        request.complete(HttpStatusCodes.NOT_FOUND);
    }
}
