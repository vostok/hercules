package ru.kontur.vostok.hercules.management.api.blacklist;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import ru.kontur.vostok.hercules.auth.AdminAuthManager;
import ru.kontur.vostok.hercules.meta.auth.blacklist.BlacklistRepository;
import ru.kontur.vostok.hercules.undertow.util.ExchangeUtil;
import ru.kontur.vostok.hercules.undertow.util.ResponseUtil;

import java.util.Optional;

/**
 * @author Gregory Koshelev
 */
public abstract class BlacklistHandler implements HttpHandler {
    protected final BlacklistRepository repository;

    protected BlacklistHandler(BlacklistRepository repository) {
        this.repository = repository;
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
