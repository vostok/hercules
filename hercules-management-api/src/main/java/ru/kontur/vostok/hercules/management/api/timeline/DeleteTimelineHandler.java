package ru.kontur.vostok.hercules.management.api.timeline;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.meta.timeline.TimelineRepository;
import ru.kontur.vostok.hercules.undertow.util.ExchangeUtil;
import ru.kontur.vostok.hercules.undertow.util.ResponseUtil;

import java.util.Optional;

/**
 * @author Gregory Koshelev
 */
public class DeleteTimelineHandler implements HttpHandler {
    private final AuthManager authManager;
    private final TimelineRepository repository;

    public DeleteTimelineHandler(AuthManager authManager, TimelineRepository repository) {
        this.authManager = authManager;
        this.repository = repository;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        Optional<String> apiKey = ExchangeUtil.extractHeaderValue(exchange, "apiKey");
        if (!apiKey.isPresent()) {
            ResponseUtil.unauthorized(exchange);
            return;
        }

        Optional<String> optionalTimeline = ExchangeUtil.extractQueryParam(exchange, "timeline");
        if (!optionalTimeline.isPresent()) {
            ResponseUtil.badRequest(exchange);
            return;
        }
        String timeline = optionalTimeline.get();

        //TODO: auth

        repository.delete(timeline);
        //TODO: delete table too
    }
}
