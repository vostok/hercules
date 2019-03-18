package ru.kontur.vostok.hercules.management.api.rule;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import ru.kontur.vostok.hercules.meta.auth.rule.RuleRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Gregory Koshelev
 */
public class ListRuleHandler extends RuleHandler {
    private final ObjectMapper mapper = new ObjectMapper();

    public ListRuleHandler(RuleRepository repository) {
        super(repository);
    }

    @Override
    public void process(HttpServerExchange exchange) throws Exception {
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        List<String> list = repository.list().stream()
                .sorted()
                .collect(Collectors.toList());
        exchange.getResponseSender().send(mapper.writeValueAsString(list));
    }
}
