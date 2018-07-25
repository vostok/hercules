package ru.kontur.vostok.hercules.management.api.timeline;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.management.api.task.CassandraTaskQueue;
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
    private final CassandraTaskQueue cassandraTaskQueue;

    public DeleteTimelineHandler(AuthManager authManager, TimelineRepository repository, CassandraTaskQueue cassandraTaskQueue) {
        this.authManager = authManager;
        this.repository = repository;
        this.cassandraTaskQueue = cassandraTaskQueue;
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

        cassandraTaskQueue.deleteTable(timeline);

        //TODO: Meta deletion may fail after successful topic deletion (no atomicity at all).
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
