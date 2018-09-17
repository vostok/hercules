package ru.kontur.vostok.hercules.management.api.sink.sentry;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.meta.sink.sentry.SentryProjectRepository;
import ru.kontur.vostok.hercules.undertow.util.ResponseUtil;

/**
 * SentryRegistryHandler
 *
 * @author Kirill Sulim
 */
public abstract class SentryRegistryHandler implements HttpHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SentryRegistryHandler.class);

    protected final ObjectMapper mapper = new ObjectMapper();

    protected final SentryProjectRepository sentryProjectRepository;

    public SentryRegistryHandler(SentryProjectRepository sentryProjectRepository) {
        this.sentryProjectRepository = sentryProjectRepository;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        try {
            process(exchange);
        } catch (Exception e) {
            LOGGER.error("Error on processing request", e);
            ResponseUtil.internalServerError(exchange);
        } finally {
            exchange.endExchange();
        }
    }

    public abstract void process(HttpServerExchange exchange) throws Exception;
}
