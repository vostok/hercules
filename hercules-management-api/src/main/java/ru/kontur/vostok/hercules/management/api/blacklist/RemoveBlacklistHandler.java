package ru.kontur.vostok.hercules.management.api.blacklist;

import io.undertow.server.HttpServerExchange;
import ru.kontur.vostok.hercules.management.api.AdminManager;
import ru.kontur.vostok.hercules.meta.auth.blacklist.BlacklistRepository;
import ru.kontur.vostok.hercules.undertow.util.ExchangeUtil;
import ru.kontur.vostok.hercules.undertow.util.ResponseUtil;

import java.util.Optional;

/**
 * @author Gregory Koshelev
 */
public class RemoveBlacklistHandler extends BlacklistHandler {
    public RemoveBlacklistHandler(AdminManager adminManager, BlacklistRepository repository) {
        super(adminManager, repository);
    }

    @Override
    public void process(HttpServerExchange exchange) throws Exception {
        Optional<String> key = ExchangeUtil.extractQueryParam(exchange, "key");
        if (!key.isPresent()) {
            ResponseUtil.badRequest(exchange);
            return;
        }

        //TODO: Validate key format

        repository.remove(key.get());
        ResponseUtil.ok(exchange);
    }
}
