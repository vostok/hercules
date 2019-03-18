package ru.kontur.vostok.hercules.management.api.blacklist;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import ru.kontur.vostok.hercules.auth.AdminAuthManager;
import ru.kontur.vostok.hercules.meta.auth.blacklist.BlacklistRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Gregory Koshelev
 */
public class ListBlacklistHandler extends BlacklistHandler {
    private final ObjectMapper mapper = new ObjectMapper();

    public ListBlacklistHandler(BlacklistRepository repository) {
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
