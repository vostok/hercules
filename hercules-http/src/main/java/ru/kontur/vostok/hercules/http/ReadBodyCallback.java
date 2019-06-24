package ru.kontur.vostok.hercules.http;

/**
 * @author Gregory Koshelev
 */
public interface ReadBodyCallback {
    void dispatch(HttpServerRequest request, byte[] bytes);
}
