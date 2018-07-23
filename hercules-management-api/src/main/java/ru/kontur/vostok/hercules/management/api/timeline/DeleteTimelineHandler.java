package ru.kontur.vostok.hercules.management.api.timeline;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.management.api.cassandra.CassandraManager;
import ru.kontur.vostok.hercules.meta.curator.DeletionResult;
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
    private final CassandraManager cassandraManager;

    public DeleteTimelineHandler(AuthManager authManager, TimelineRepository repository, CassandraManager cassandraManager) {
        this.authManager = authManager;
        this.repository = repository;
        this.cassandraManager = cassandraManager;
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

        // Delete table
        cassandraManager.deleteTable(timeline);

        // Delete metadata
        DeletionResult deletionResult = repository.delete(timeline);
        if (!deletionResult.isSuccess()) {
            switch (deletionResult.getStatus()) {
                case NOT_EXIST:
                    ResponseUtil.notFound(exchange);
                    return;
                case UNKNOWN:
                    ResponseUtil.internalServerError(exchange);
                    return;
            }
        }

        ResponseUtil.ok(exchange);
    }
}
