package ru.kontur.vostok.hercules.management.api.blacklist;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import ru.kontur.vostok.hercules.management.api.AdminManager;
import ru.kontur.vostok.hercules.meta.blacklist.BlacklistRepository;

import java.util.List;

/**
 * @author Gregory Koshelev
 */
public class ListBlacklistHandler extends BlacklistHandler {
    private final ObjectMapper mapper = new ObjectMapper();

    public ListBlacklistHandler(AdminManager adminManager, BlacklistRepository repository) {
        super(adminManager, repository);
    }

    @Override
    public void process(HttpServerExchange exchange) throws Exception {
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        List<String> list = repository.list();
        exchange.getResponseSender().send(mapper.writeValueAsString(list));
    }
}
