package ru.kontur.vostok.hercules.http;

/**
 * @author Gregory Koshelev
 */
public interface AsyncHttpHandler {
    void handleAsync(HttpServerRequest request);
}
