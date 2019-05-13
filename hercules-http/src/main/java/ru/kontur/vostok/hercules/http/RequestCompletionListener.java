package ru.kontur.vostok.hercules.http;

import ru.kontur.vostok.hercules.http.HttpServerRequest;

/**
 * @author Gregory Koshelev
 */
public interface RequestCompletionListener {
    void onComplete(HttpServerRequest request);
}
