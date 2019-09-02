package ru.kontur.vostok.hercules.management.api.rule;

import ru.kontur.vostok.hercules.http.handler.HttpHandler;
import ru.kontur.vostok.hercules.meta.auth.rule.RuleRepository;

/**
 * @author Gregory Koshelev
 */
public abstract class RuleHandler implements HttpHandler {
    protected final RuleRepository repository;

    protected RuleHandler(RuleRepository repository) {
        this.repository = repository;
    }
}
