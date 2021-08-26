package ru.kontur.vostok.hercules.management.api.timeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.auth.AuthProvider;
import ru.kontur.vostok.hercules.auth.AuthResult;
import ru.kontur.vostok.hercules.auth.AuthUtil;
import ru.kontur.vostok.hercules.curator.exception.CuratorException;
import ru.kontur.vostok.hercules.http.ContentTypes;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.http.handler.HttpHandler;
import ru.kontur.vostok.hercules.http.query.QueryUtil;
import ru.kontur.vostok.hercules.management.api.HttpAsyncApiHelper;
import ru.kontur.vostok.hercules.management.api.QueryParameters;
import ru.kontur.vostok.hercules.meta.task.TaskFuture;
import ru.kontur.vostok.hercules.meta.task.TaskQueue;
import ru.kontur.vostok.hercules.meta.task.timeline.TimelineTask;
import ru.kontur.vostok.hercules.meta.task.timeline.TimelineTaskType;
import ru.kontur.vostok.hercules.meta.timeline.Timeline;
import ru.kontur.vostok.hercules.meta.timeline.TimelineRepository;
import ru.kontur.vostok.hercules.util.parameter.Parameter;

import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class DeleteTimelineHandler implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteTimelineHandler.class);

    private final AuthProvider authProvider;
    private final TaskQueue<TimelineTask> taskQueue;
    private final TimelineRepository repository;

    public DeleteTimelineHandler(AuthProvider authProvider, TaskQueue<TimelineTask> taskQueue, TimelineRepository repository) {
        this.authProvider = authProvider;
        this.taskQueue = taskQueue;
        this.repository = repository;
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

        try {
            if (!repository.exists(timelineName.get())) {
                request.complete(HttpStatusCodes.NOT_FOUND, ContentTypes.TEXT_PLAIN_UTF_8,
                        "Cannot find timeline with name " + timelineName.get());
                return;
            }
        } catch (CuratorException ex) {
            LOGGER.error("Deserialization of Timeline exception", ex);
            request.complete(HttpStatusCodes.INTERNAL_SERVER_ERROR);
            return;
        }

        Timeline stub = new Timeline();
        stub.setName(timelineName.get());

        TaskFuture taskFuture =
                taskQueue.submit(
                        new TimelineTask(stub, TimelineTaskType.DELETE),
                        stub.getName(),
                        QueryUtil.get(QueryParameters.TIMEOUT_MS, request).get(),
                        TimeUnit.MILLISECONDS);
        HttpAsyncApiHelper.awaitAndComplete(taskFuture, request);
    }
}
