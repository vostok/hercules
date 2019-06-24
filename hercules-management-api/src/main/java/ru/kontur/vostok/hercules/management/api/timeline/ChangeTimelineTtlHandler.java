package ru.kontur.vostok.hercules.management.api.timeline;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.auth.AuthResult;
import ru.kontur.vostok.hercules.curator.exception.CuratorInternalException;
import ru.kontur.vostok.hercules.curator.exception.CuratorUnknownException;
import ru.kontur.vostok.hercules.meta.serialization.DeserializationException;
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
 * @author Vladimir Tsypaev
 */
public class ChangeTimelineTtlHandler implements HttpHandler {

    private final AuthManager authManager;
    private final TaskQueue<TimelineTask> taskQueue;
    private final TimelineRepository timelineRepository;

    public ChangeTimelineTtlHandler(AuthManager authManager, TaskQueue<TimelineTask> taskQueue, TimelineRepository timelineRepository) {
        this.authManager = authManager;
        this.taskQueue = taskQueue;
        this.timelineRepository = timelineRepository;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        Optional<String> optionalApiKey = ExchangeUtil.extractHeaderValue(exchange, "apiKey");
        if (!optionalApiKey.isPresent()) {
            ResponseUtil.unauthorized(exchange);
            return;
        }

        final String apiKey = optionalApiKey.get();

        Optional<String> optionalTimelineName = ExchangeUtil.extractQueryParam(exchange, "timeline");
        if (!optionalTimelineName.isPresent()) {
            ResponseUtil.badRequest(exchange);
            return;
        }
        final String timelineName = optionalTimelineName.get();

        AuthResult authResult = authManager.authManage(apiKey, timelineName);
        if (!authResult.isSuccess()) {
            if (authResult.isUnknown()) {
                ResponseUtil.unauthorized(exchange);
                return;
            }
            ResponseUtil.forbidden(exchange);
            return;
        }

        Optional<Long> optionalNewTtl = ExchangeUtil.extractLongQueryParam(exchange, "newTtl");
        if (!optionalNewTtl.isPresent()) {
            ResponseUtil.badRequest(exchange);
            return;
        }
        long newTtl = optionalNewTtl.get();

        Optional<Timeline> optionalTimeline;
        try {
            optionalTimeline = timelineRepository.read(timelineName);
        } catch (CuratorUnknownException | CuratorInternalException | DeserializationException e) {
            ResponseUtil.internalServerError(exchange);
            return;
        }
        if (!optionalTimeline.isPresent()) {
            ResponseUtil.notFound(exchange);
            return;
        }
        Timeline timeline = optionalTimeline.get();

        timeline.setTtl(newTtl);
        TaskFuture taskFuture =
                taskQueue.submit(
                        new TimelineTask(timeline, TimelineTaskType.CHANGE_TTL),
                        timeline.getName(),
                        10_000L,//TODO: Move to Properties or add timeout query param
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
            ResponseUtil.requestTimeout(exchange);
            return;
        }
        ResponseUtil.ok(exchange);
    }
}
