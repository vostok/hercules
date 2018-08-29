package ru.kontur.vostok.hercules.http.handler;

import ru.kontur.vostok.hercules.http.HttpServerRequest;

/**
 * @author Gregory Koshelev
 */
public interface AsyncHttpHandler {
    void handleAsync(HttpServerRequest request);
}
