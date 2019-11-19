package ru.kontur.vostok.hercules.http;

/**
 * @author Gregory Koshelev
 */
public interface IoCallback {
    void onComplete(HttpServerRequest request);
}
