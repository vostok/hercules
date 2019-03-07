package ru.kontur.vostok.hercules.management.api.timeline;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.auth.AuthResult;
import ru.kontur.vostok.hercules.curator.result.CreationResult;
import ru.kontur.vostok.hercules.meta.task.TaskFuture;
import ru.kontur.vostok.hercules.meta.task.TaskQueue;
import ru.kontur.vostok.hercules.meta.task.timeline.TimelineTask;
import ru.kontur.vostok.hercules.meta.task.timeline.TimelineTaskType;
import ru.kontur.vostok.hercules.meta.timeline.Timeline;
import ru.kontur.vostok.hercules.meta.timeline.TimelineRepository;
import ru.kontur.vostok.hercules.undertow.util.ExchangeUtil;
import ru.kontur.vostok.hercules.undertow.util.ResponseUtil;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class DeleteTimelineHandler implements HttpHandler {
    private final AuthManager authManager;
    private final TaskQueue<TimelineTask> taskQueue;
    private final TimelineRepository repository;

    public DeleteTimelineHandler(AuthManager authManager, TaskQueue<TimelineTask> taskQueue, TimelineRepository repository) {
        this.authManager = authManager;
        this.taskQueue = taskQueue;
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

        AuthResult authResult = authManager.authManage(apiKey.get(), timeline);
        if (!authResult.isSuccess()) {
            if (authResult.isUnknown()) {
                ResponseUtil.unauthorized(exchange);
                return;
            }
            ResponseUtil.forbidden(exchange);
            return;
        }

        if (!repository.exists(timeline)) {
            ResponseUtil.notFound(exchange);
            return;
        }

        Timeline stub = new Timeline();
        stub.setName(timeline);

        TaskFuture taskFuture =
                taskQueue.submit(
                        new TimelineTask(stub, TimelineTaskType.DELETE),
                        stub.getName(),
                        10_000L,//TODO: Move to Properties
                        TimeUnit.MILLISECONDS);
        if (taskFuture.isFailed()) {
            ResponseUtil.internalServerError(exchange);
            return;
        }

        if (!ExchangeUtil.extractQueryParam(exchange, "async").isPresent()) {
            taskFuture.await();
            if (taskFuture.isDone()) {
                ResponseUtil.ok(exchange);
                return;
            }
            ResponseUtil.internalServerError(exchange);
            return;
        }
        ResponseUtil.ok(exchange);
    }
}
