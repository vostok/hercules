package ru.kontur.vostok.hercules.http.handler;

import ru.kontur.vostok.hercules.http.HttpServerRequest;

/**
 * Wrapper wraps handler with another handler.
 *
 * @author Gregory Koshelev
 */
public interface HandlerWrapper {
    HttpHandler wrap(HttpHandler handler);
}
