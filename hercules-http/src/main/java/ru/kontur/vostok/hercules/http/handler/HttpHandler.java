package ru.kontur.vostok.hercules.http.handler;

import ru.kontur.vostok.hercules.http.HttpServerRequest;

/**
 * @author Gregory Koshelev
 */
public interface HttpHandler {
    void handle(HttpServerRequest request);
}
