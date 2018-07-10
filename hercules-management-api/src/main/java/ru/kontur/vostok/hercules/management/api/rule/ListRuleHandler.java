package ru.kontur.vostok.hercules.management.api.rule;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import ru.kontur.vostok.hercules.management.api.AdminManager;
import ru.kontur.vostok.hercules.meta.rule.RuleRepository;

import java.util.List;

/**
 * @author Gregory Koshelev
 */
public class ListRuleHandler extends RuleHandler {
    private final ObjectMapper mapper = new ObjectMapper();

    public ListRuleHandler(AdminManager adminManager, RuleRepository repository) {
        super(adminManager, repository);
    }

    @Override
    public void process(HttpServerExchange exchange) throws Exception {
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        List<String> list = repository.list();
        exchange.getResponseSender().send(mapper.writeValueAsString(list));
    }
}
