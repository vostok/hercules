package ru.kontur.vostok.hercules.http.handler;

/**
 * Wrapper wraps handler with another handler.
 *
 * @author Gregory Koshelev
 */
public interface HandlerWrapper {
    HttpHandler wrap(HttpHandler handler);
}
