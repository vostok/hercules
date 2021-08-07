package ru.kontur.vostok.hercules.management.api.timeline;

import ru.kontur.vostok.hercules.auth.AuthProvider;
import ru.kontur.vostok.hercules.auth.AuthResult;
import ru.kontur.vostok.hercules.auth.AuthUtil;
import ru.kontur.vostok.hercules.curator.exception.CuratorException;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.http.handler.HttpHandler;
import ru.kontur.vostok.hercules.http.query.QueryUtil;
import ru.kontur.vostok.hercules.management.api.HttpAsyncApiHelper;
import ru.kontur.vostok.hercules.management.api.QueryParameters;
import ru.kontur.vostok.hercules.meta.serialization.DeserializationException;
import ru.kontur.vostok.hercules.meta.task.TaskFuture;
import ru.kontur.vostok.hercules.meta.task.TaskQueue;
import ru.kontur.vostok.hercules.meta.task.timeline.TimelineTask;
import ru.kontur.vostok.hercules.meta.task.timeline.TimelineTaskType;
import ru.kontur.vostok.hercules.meta.timeline.Timeline;
import ru.kontur.vostok.hercules.meta.timeline.TimelineRepository;
import ru.kontur.vostok.hercules.util.parameter.Parameter;

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
        Parameter<String>.ParameterValue timelineName = QueryUtil.get(QueryParameters.TIMELINE, request);
        if (QueryUtil.tryCompleteRequestIfError(request, timelineName)) {
            return;
        }

        AuthResult authResult = authProvider.authManage(request, timelineName.get());
        if (AuthUtil.tryCompleteRequestIfUnsuccessfulAuth(request, authResult)) {
            return;
        }

        Parameter<Long>.ParameterValue newTtl = QueryUtil.get(QueryParameters.NEW_TTL, request);
        if (QueryUtil.tryCompleteRequestIfError(request, newTtl)) {
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
                        QueryUtil.get(QueryParameters.TIMEOUT_MS, request).get(),
                        TimeUnit.MILLISECONDS);
        HttpAsyncApiHelper.awaitAndComplete(taskFuture, request);
    }
}
