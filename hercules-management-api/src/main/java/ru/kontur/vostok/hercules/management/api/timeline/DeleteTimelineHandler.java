package ru.kontur.vostok.hercules.management.api.timeline;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.curator.result.CreationResult;
import ru.kontur.vostok.hercules.meta.task.timeline.TimelineTask;
import ru.kontur.vostok.hercules.meta.task.timeline.TimelineTaskRepository;
import ru.kontur.vostok.hercules.meta.task.timeline.TimelineTaskType;
import ru.kontur.vostok.hercules.meta.timeline.Timeline;
import ru.kontur.vostok.hercules.undertow.util.ExchangeUtil;
import ru.kontur.vostok.hercules.undertow.util.ResponseUtil;

import java.util.Optional;

/**
 * @author Gregory Koshelev
 */
public class DeleteTimelineHandler implements HttpHandler {
    private final AuthManager authManager;
    private final TimelineTaskRepository repository;

    public DeleteTimelineHandler(AuthManager authManager, TimelineTaskRepository repository) {
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

        Timeline stub = new Timeline();
        stub.setName(timeline);

        CreationResult creationResult =
                repository.create(new TimelineTask(stub, TimelineTaskType.DELETE), stub.getName());
        if (!creationResult.isSuccess()) {
            ResponseUtil.internalServerError(exchange);
            return;
        }

        //TODO: Wait for deletion if needed
        ResponseUtil.ok(exchange);
    }
}
