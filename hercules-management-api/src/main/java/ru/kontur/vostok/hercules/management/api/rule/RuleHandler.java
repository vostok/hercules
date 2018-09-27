package ru.kontur.vostok.hercules.management.api.rule;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import ru.kontur.vostok.hercules.meta.auth.rule.RuleRepository;

/**
 * @author Gregory Koshelev
 */
public abstract class RuleHandler implements HttpHandler {
    protected final RuleRepository repository;

    protected RuleHandler(RuleRepository repository) {
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
