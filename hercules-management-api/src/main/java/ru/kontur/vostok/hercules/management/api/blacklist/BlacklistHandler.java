package ru.kontur.vostok.hercules.management.api.blacklist;

import ru.kontur.vostok.hercules.http.handler.HttpHandler;
import ru.kontur.vostok.hercules.meta.auth.blacklist.BlacklistRepository;

/**
 * @author Gregory Koshelev
 */
public abstract class BlacklistHandler implements HttpHandler {
    protected final BlacklistRepository repository;

    protected BlacklistHandler(BlacklistRepository repository) {
        this.repository = repository;
    }
}
