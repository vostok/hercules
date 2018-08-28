package ru.kontur.vostok.hercules.http;

/**
 * @author Gregory Koshelev
 */
public final class NotFoundHandler implements AsyncHttpHandler {
    @Override
    public void handleAsync(HttpServerRequest request) {
        request.complete(HttpStatusCodes.NOT_FOUND);
    }
}
