package ru.kontur.vostok.hercules.undertow.util;

import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.throttling.ThrottledBy;
import ru.kontur.vostok.hercules.throttling.ThrottledRequestProcessor;

/**
 * @author Gregory Koshelev
 */
public class DefaultUndertowThrottledRequestProcessor implements ThrottledRequestProcessor<HttpServerExchange> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultUndertowThrottledRequestProcessor.class);

    @Override
    public void processAsync(HttpServerExchange request, ThrottledBy throttledBy) {
        LOGGER.warn("Throttle request by " + throttledBy);
        ResponseUtil.serviceUnavailable(request);
    }
}
