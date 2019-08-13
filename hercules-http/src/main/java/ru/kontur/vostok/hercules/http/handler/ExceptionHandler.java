package ru.kontur.vostok.hercules.http.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;

/**
 * @author Gregory Koshelev
 */
public class ExceptionHandler implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionHandler.class);

    private final HttpHandler handler;

    public ExceptionHandler(HttpHandler handler) {
        this.handler = handler;
    }

    @Override
    public void handle(HttpServerRequest request) {
        try {
            handler.handle(request);
        } catch (Throwable throwable) {
            LOGGER.error("Internal Server Error due to exception", throwable);
            request.complete(HttpStatusCodes.INTERNAL_SERVER_ERROR);
        }
    }
}
