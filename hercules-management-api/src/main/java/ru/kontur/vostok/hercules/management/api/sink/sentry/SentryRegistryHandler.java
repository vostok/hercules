package ru.kontur.vostok.hercules.management.api.sink.sentry;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import ru.kontur.vostok.hercules.management.api.AdminManager;
import ru.kontur.vostok.hercules.meta.sink.sentry.SentryProjectRepository;
import ru.kontur.vostok.hercules.undertow.util.ExchangeUtil;
import ru.kontur.vostok.hercules.undertow.util.ResponseUtil;

import java.util.Optional;

/**
 * SentryRegistryHandler
 *
 * @author Kirill Sulim
 */
public abstract class SentryRegistryHandler implements HttpHandler {

    private final AdminManager adminManager;
    private final SentryProjectRepository sentryProjectRepository;

    public SentryRegistryHandler(AdminManager adminManager, SentryProjectRepository sentryProjectRepository) {
        this.adminManager = adminManager;
        this.sentryProjectRepository = sentryProjectRepository;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        Optional<String> apiKey = ExchangeUtil.extractHeaderValue(exchange, "apiKey");
        if (!apiKey.isPresent() || !adminManager.auth(apiKey.get()).isSuccess()) {
            ResponseUtil.unauthorized(exchange);
            return;
        }

        try {
            process(exchange);
        } finally {
            exchange.endExchange();
        }
    }

    public abstract void process(HttpServerExchange exchange) throws Exception;
}
