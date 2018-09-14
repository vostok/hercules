package ru.kontur.vostok.hercules.management.api.sink.sentry;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import ru.kontur.vostok.hercules.meta.sink.sentry.SentryProjectRepository;

/**
 * SentryRegistryHandler
 *
 * @author Kirill Sulim
 */
public abstract class SentryRegistryHandler implements HttpHandler {

    private final SentryProjectRepository sentryProjectRepository;

    public SentryRegistryHandler(SentryProjectRepository sentryProjectRepository) {
        this.sentryProjectRepository = sentryProjectRepository;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        try {
            process(exchange);
        } finally {
            exchange.endExchange();
        }
    }

    public abstract void process(HttpServerExchange exchange) throws Exception;
}
