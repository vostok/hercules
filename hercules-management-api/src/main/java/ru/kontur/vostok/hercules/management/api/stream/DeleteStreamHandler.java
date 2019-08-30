package ru.kontur.vostok.hercules.management.api.stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.auth.AuthResult;
import ru.kontur.vostok.hercules.curator.exception.CuratorException;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.http.MimeTypes;
import ru.kontur.vostok.hercules.http.handler.HttpHandler;
import ru.kontur.vostok.hercules.http.query.QueryUtil;
import ru.kontur.vostok.hercules.management.api.HttpAsyncApiHelper;
import ru.kontur.vostok.hercules.management.api.QueryParameters;
import ru.kontur.vostok.hercules.meta.stream.BaseStream;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.meta.task.TaskFuture;
import ru.kontur.vostok.hercules.meta.task.TaskQueue;
import ru.kontur.vostok.hercules.meta.task.stream.StreamTask;
import ru.kontur.vostok.hercules.meta.task.stream.StreamTaskType;
import ru.kontur.vostok.hercules.util.parameter.ParameterValue;

import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class DeleteStreamHandler implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteStreamHandler.class);

    private final AuthManager authManager;
    private final TaskQueue<StreamTask> taskQueue;
    private final StreamRepository streamRepository;

    public DeleteStreamHandler(AuthManager authManager, TaskQueue<StreamTask> taskQueue, StreamRepository streamRepository) {
        this.authManager = authManager;
        this.taskQueue = taskQueue;
        this.streamRepository = streamRepository;
    }

    @Override
    public void handle(HttpServerRequest request) {
        String apiKey = request.getHeader("apiKey");
        if (apiKey == null) {
            request.complete(HttpStatusCodes.UNAUTHORIZED);
            return;
        }
        ParameterValue<String> streamName = QueryUtil.get(QueryParameters.STREAM, request);
        if (streamName.isError()) {
            request.complete(
                    HttpStatusCodes.BAD_REQUEST,
                    MimeTypes.TEXT_PLAIN,
                    "Parameter " + QueryParameters.STREAM.name() + " error: " + streamName.result().error());
            return;
        }

        AuthResult authResult = authManager.authManage(apiKey, streamName.get());
        if (!authResult.isSuccess()) {
            if (authResult.isUnknown()) {
                request.complete(HttpStatusCodes.UNAUTHORIZED);
                return;
            }
            request.complete(HttpStatusCodes.FORBIDDEN);
            return;
        }

        try {
            if (!streamRepository.exists(streamName.get())) {
                request.complete(HttpStatusCodes.NOT_FOUND);
                return;
            }
        } catch (CuratorException ex) {
            LOGGER.error("Stream existence check failed with exception", ex);
            request.complete(HttpStatusCodes.INTERNAL_SERVER_ERROR);
            return;
        }

        BaseStream stub = new BaseStream();
        stub.setName(streamName.get());

        TaskFuture taskFuture =
                taskQueue.submit(
                        new StreamTask(stub, StreamTaskType.DELETE),
                        stub.getName(),
                10_000L,//TODO: Move to Properties
                TimeUnit.MILLISECONDS);
        HttpAsyncApiHelper.awaitAndComplete(taskFuture, request);
    }
}
