package ru.kontur.vostok.hercules.management.api.blacklist;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import ru.kontur.vostok.hercules.management.api.AdminManager;
import ru.kontur.vostok.hercules.meta.auth.blacklist.BlacklistRepository;
import ru.kontur.vostok.hercules.undertow.util.ExchangeUtil;
import ru.kontur.vostok.hercules.undertow.util.ResponseUtil;

import java.util.Optional;

/**
 * @author Gregory Koshelev
 */
public abstract class BlacklistHandler implements HttpHandler {
    private final AdminManager adminManager;
    protected final BlacklistRepository repository;

    protected BlacklistHandler(AdminManager adminManager, BlacklistRepository repository) {
        this.adminManager = adminManager;
        this.repository = repository;
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
