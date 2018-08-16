package ru.kontur.vostok.hercules.management.api.rule;

import io.undertow.server.HttpServerExchange;
import ru.kontur.vostok.hercules.management.api.AdminManager;
import ru.kontur.vostok.hercules.meta.auth.rule.RuleRepository;
import ru.kontur.vostok.hercules.undertow.util.ExchangeUtil;
import ru.kontur.vostok.hercules.undertow.util.ResponseUtil;

import java.util.Optional;

/**
 * @author Gregory Koshelev
 */
public class SetRuleHandler extends RuleHandler {
    public SetRuleHandler(AdminManager adminManager, RuleRepository repository) {
        super(adminManager, repository);
    }

    @Override
    public void process(HttpServerExchange exchange) throws Exception {
        Optional<String> key = ExchangeUtil.extractQueryParam(exchange, "key");
        Optional<String> pattern = ExchangeUtil.extractQueryParam(exchange, "pattern");
        Optional<String> rights = ExchangeUtil.extractQueryParam(exchange, "rights");

        if (!key.isPresent() || !pattern.isPresent() || !rights.isPresent()) {
            exchange.setStatusCode(400);
            exchange.endExchange();
            return;
        }

        //TODO: Validate query parameters

        String ruleRead = key.get() + '.' + pattern.get() + '.' + "read";
        String ruleWrite = key.get() + '.' + pattern.get() + '.' + "write";
        String ruleManage = key.get() + '.' + pattern.get() + '.' + "manage";

        String mask = rights.get();

        if (mask.charAt(0) == 'r') {
            repository.create(ruleRead);
        } else {
            repository.delete(ruleRead);
        }
        if (mask.charAt(1) == 'w') {
            repository.create(ruleWrite);
        } else {
            repository.delete(ruleWrite);
        }
        if (mask.charAt(2) == 'm') {
            repository.create(ruleManage);
        } else {
            repository.delete(ruleManage);
        }

        ResponseUtil.ok(exchange);
    }
}
