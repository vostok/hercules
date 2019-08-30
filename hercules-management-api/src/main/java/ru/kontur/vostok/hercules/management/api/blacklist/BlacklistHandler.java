package ru.kontur.vostok.hercules.management.api.blacklist;

import ru.kontur.vostok.hercules.http.HttpServerRequest;
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

    @Override
    public void handle(HttpServerRequest request) {
        try {
            process(request);
        } finally {
            request.complete();
        }
    }

    /**
     * Non async method to process HTTP request.
     *
     * @param request the HTTP request
     */
    public abstract void process(HttpServerRequest request);
}
