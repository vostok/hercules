package ru.kontur.vostok.hercules.management.api.timeline;

import ru.kontur.vostok.hercules.auth.AuthResult;
import ru.kontur.vostok.hercules.curator.exception.CuratorException;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.http.MimeTypes;
import ru.kontur.vostok.hercules.http.handler.HttpHandler;
import ru.kontur.vostok.hercules.http.query.QueryUtil;
import ru.kontur.vostok.hercules.management.api.HttpAsyncApiHelper;
import ru.kontur.vostok.hercules.management.api.QueryParameters;
import ru.kontur.vostok.hercules.management.api.auth.AuthProvider;
import ru.kontur.vostok.hercules.meta.serialization.DeserializationException;
import ru.kontur.vostok.hercules.meta.task.TaskFuture;
import ru.kontur.vostok.hercules.meta.task.TaskQueue;
import ru.kontur.vostok.hercules.meta.task.timeline.TimelineTask;
import ru.kontur.vostok.hercules.meta.task.timeline.TimelineTaskType;
import ru.kontur.vostok.hercules.meta.timeline.Timeline;
import ru.kontur.vostok.hercules.meta.timeline.TimelineRepository;
import ru.kontur.vostok.hercules.util.parameter.ParameterValue;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author Vladimir Tsypaev
 */
public class ChangeTimelineTtlHandler implements HttpHandler {

    private final AuthProvider authProvider;
    private final TaskQueue<TimelineTask> taskQueue;
    private final TimelineRepository timelineRepository;

    public ChangeTimelineTtlHandler(AuthProvider authProvider, TaskQueue<TimelineTask> taskQueue, TimelineRepository timelineRepository) {
        this.authProvider = authProvider;
        this.taskQueue = taskQueue;
        this.timelineRepository = timelineRepository;
    }

    @Override
    public void handle(HttpServerRequest request) {
        ParameterValue<String> timelineName = QueryUtil.get(QueryParameters.TIMELINE, request);
        if (timelineName.isError()) {
            request.complete(
                    HttpStatusCodes.BAD_REQUEST,
                    MimeTypes.TEXT_PLAIN,
                    "Parameter " + QueryParameters.TIMELINE.name() + " error: " + timelineName.result().error());
            return;
        }

        AuthResult authResult = authProvider.authManage(request, timelineName.get());
        if (!authResult.isSuccess()) {
            if (authResult.isUnknown()) {
                request.complete(HttpStatusCodes.UNAUTHORIZED);
                return;
            }
            request.complete(HttpStatusCodes.FORBIDDEN);
            return;
        }

        ParameterValue<Long> newTtl = QueryUtil.get(QueryParameters.NEW_TTL, request);
        if (newTtl.isError()) {
            request.complete(
                    HttpStatusCodes.BAD_REQUEST,
                    MimeTypes.TEXT_PLAIN,
                    "Parameter " + QueryParameters.NEW_TTL.name() + " error: " + newTtl.result().error());
            return;
        }

        Timeline timeline;
        try {
            Optional<Timeline> optionalTimeline = timelineRepository.read(timelineName.get());
            if (!optionalTimeline.isPresent()) {
                request.complete(HttpStatusCodes.NOT_FOUND);
                return;
            }
            timeline = optionalTimeline.get();
        } catch (CuratorException | DeserializationException e) {
            request.complete(HttpStatusCodes.INTERNAL_SERVER_ERROR);
            return;
        }

        timeline.setTtl(newTtl.get());
        TaskFuture taskFuture =
                taskQueue.submit(
                        new TimelineTask(timeline, TimelineTaskType.CHANGE_TTL),
                        timeline.getName(),
                        10_000L,//TODO: Move to Properties or add timeout query param
                        TimeUnit.MILLISECONDS);
        HttpAsyncApiHelper.awaitAndComplete(taskFuture, request);
    }
}
