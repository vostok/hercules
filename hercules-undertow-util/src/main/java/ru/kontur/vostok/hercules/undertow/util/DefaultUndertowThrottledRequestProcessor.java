package ru.kontur.vostok.hercules.undertow.util;

import io.undertow.server.HttpServerExchange;
import ru.kontur.vostok.hercules.throttling.ThrottledBy;
import ru.kontur.vostok.hercules.throttling.ThrottledRequestProcessor;

/**
 * @author Gregory Koshelev
 */
public class DefaultUndertowThrottledRequestProcessor implements ThrottledRequestProcessor<HttpServerExchange> {
    @Override
    public void processAsync(HttpServerExchange request, ThrottledBy throttledBy) {
        ResponseUtil.serviceUnavailable(request);
    }
}
