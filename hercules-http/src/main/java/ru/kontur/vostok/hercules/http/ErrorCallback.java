package ru.kontur.vostok.hercules.http;

/**
 * @author Gregory Koshelev
 */
public interface ErrorCallback {
    void error(HttpServerRequest request, HttpServerRequestException exception);
}
