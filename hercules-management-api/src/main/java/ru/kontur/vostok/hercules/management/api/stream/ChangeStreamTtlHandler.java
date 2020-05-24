package ru.kontur.vostok.hercules.management.api.stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.auth.AuthProvider;
import ru.kontur.vostok.hercules.auth.AuthResult;
import ru.kontur.vostok.hercules.curator.exception.CuratorException;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.http.MimeTypes;
import ru.kontur.vostok.hercules.http.handler.HttpHandler;
import ru.kontur.vostok.hercules.http.query.QueryUtil;
import ru.kontur.vostok.hercules.management.api.HttpAsyncApiHelper;
import ru.kontur.vostok.hercules.management.api.QueryParameters;
import ru.kontur.vostok.hercules.meta.serialization.DeserializationException;
import ru.kontur.vostok.hercules.meta.stream.Stream;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.meta.task.TaskFuture;
import ru.kontur.vostok.hercules.meta.task.TaskQueue;
import ru.kontur.vostok.hercules.meta.task.stream.StreamTask;
import ru.kontur.vostok.hercules.meta.task.stream.StreamTaskType;
import ru.kontur.vostok.hercules.util.parameter.ParameterValue;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author Vladimir Tsypaev
 */
public class ChangeStreamTtlHandler implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChangeStreamTtlHandler.class);

    private final AuthProvider authProvider;
    private final TaskQueue<StreamTask> taskQueue;
    private final StreamRepository streamRepository;

    public ChangeStreamTtlHandler(AuthProvider authProvider, TaskQueue<StreamTask> taskQueue, StreamRepository streamRepository) {
        this.authProvider = authProvider;
        this.taskQueue = taskQueue;
        this.streamRepository = streamRepository;
    }

    @Override
    public void handle(HttpServerRequest request) {
        ParameterValue<String> streamName = QueryUtil.get(QueryParameters.STREAM, request);
        if (streamName.isError()) {
            request.complete(
                    HttpStatusCodes.BAD_REQUEST,
                    MimeTypes.TEXT_PLAIN,
                    "Parameter " + QueryParameters.STREAM.name() + " error: " + streamName.result().error());
            return;
        }

        AuthResult authResult = authProvider.authManage(request, streamName.get());
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

        Stream stream;
        try {
            Optional<Stream> optionalStream = streamRepository.read(streamName.get());
            if (!optionalStream.isPresent()) {
                request.complete(HttpStatusCodes.NOT_FOUND);
                return;
            }
            stream = optionalStream.get();
        } catch (CuratorException ex) {
            LOGGER.error("Curator exception when read Stream", ex);
            request.complete(HttpStatusCodes.INTERNAL_SERVER_ERROR);
            return;
        } catch (DeserializationException ex) {
            LOGGER.error("Deserialization exception of Stream", ex);
            request.complete(HttpStatusCodes.INTERNAL_SERVER_ERROR);
            return;
        }

        stream.setTtl(newTtl.get());
        TaskFuture taskFuture =
                taskQueue.submit(
                        new StreamTask(stream, StreamTaskType.CHANGE_TTL),
                        stream.getName(),
                        QueryUtil.get(QueryParameters.TIMEOUT_MS, request).get(),
                        TimeUnit.MILLISECONDS);
        HttpAsyncApiHelper.awaitAndComplete(taskFuture, request);
    }
}
