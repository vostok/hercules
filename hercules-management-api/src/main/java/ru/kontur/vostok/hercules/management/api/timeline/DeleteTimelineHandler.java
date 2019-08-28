package ru.kontur.vostok.hercules.management.api.timeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.auth.AuthResult;
import ru.kontur.vostok.hercules.curator.exception.CuratorException;
import ru.kontur.vostok.hercules.curator.exception.CuratorUnknownException;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.http.MimeTypes;
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
import ru.kontur.vostok.hercules.util.parameter.ParameterValue;

import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class DeleteTimelineHandler implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteTimelineHandler.class);

    private final AuthManager authManager;
    private final TaskQueue<TimelineTask> taskQueue;
    private final TimelineRepository repository;

    public DeleteTimelineHandler(AuthManager authManager, TaskQueue<TimelineTask> taskQueue, TimelineRepository repository) {
        this.authManager = authManager;
        this.taskQueue = taskQueue;
        this.repository = repository;
    }

    @Override
    public void handle(HttpServerRequest request) {
        String apiKey = request.getHeader("apiKey");
        if (apiKey == null) {
            request.complete(HttpStatusCodes.UNAUTHORIZED);
            return;
        }
        ParameterValue<String> timelineName = QueryUtil.get(QueryParameters.TIMELINE, request);
        if (timelineName.isError()) {
            request.complete(
                    HttpStatusCodes.BAD_REQUEST,
                    MimeTypes.TEXT_PLAIN,
                    "Parameter " + QueryParameters.TIMELINE.name() + " error: " + timelineName.result().error());
            return;
        }

        AuthResult authResult = authManager.authManage(apiKey, timelineName.get());
        if (!authResult.isSuccess()) {
            if (authResult.isUnknown()) {
                request.complete(HttpStatusCodes.UNAUTHORIZED);
                return;
            }
            request.complete(HttpStatusCodes.FORBIDDEN);
            return;
        }

        try {
            if (!repository.exists(timelineName.get())) {
                request.complete(HttpStatusCodes.NOT_FOUND);
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
                        10_000L,//TODO: Move to Properties
                        TimeUnit.MILLISECONDS);
        HttpAsyncApiHelper.awaitAndComplete(taskFuture, request);
    }
}
