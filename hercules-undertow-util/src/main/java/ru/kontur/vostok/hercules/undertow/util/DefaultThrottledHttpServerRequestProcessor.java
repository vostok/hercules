package ru.kontur.vostok.hercules.undertow.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.throttling.ThrottledBy;
import ru.kontur.vostok.hercules.throttling.ThrottledRequestProcessor;

/**
 * @author Gregory Koshelev
 */
public class DefaultThrottledHttpServerRequestProcessor implements ThrottledRequestProcessor<HttpServerRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultThrottledHttpServerRequestProcessor.class);

    @Override
    public void process(HttpServerRequest request, ThrottledBy throttledBy) {
        LOGGER.warn("Throttle request by " + throttledBy);
        request.complete(HttpStatusCodes.SERVICE_UNAVAILABLE);
    }
}
