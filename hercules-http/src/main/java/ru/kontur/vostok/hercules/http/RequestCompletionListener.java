package ru.kontur.vostok.hercules.http;

/**
 * @author Gregory Koshelev
 */
public interface RequestCompletionListener {
    void onComplete(HttpServerRequest request);
}
