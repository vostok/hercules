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
 * @author Gregory Koshelev
 */
public class IncreasePartitionsStreamHandler implements HttpHandler {
    private static Logger LOGGER = LoggerFactory.getLogger(IncreasePartitionsStreamHandler.class);

    private final AuthManager authManager;
    private final TaskQueue<StreamTask> taskQueue;
    private final StreamRepository streamRepository;

    public IncreasePartitionsStreamHandler(AuthManager authManager, TaskQueue<StreamTask> taskQueue, StreamRepository streamRepository) {
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

        ParameterValue<Integer> newPartitions = QueryUtil.get(QueryParameters.NEW_PARTITIONS, request);
        if (newPartitions.isError()) {
            request.complete(
                    HttpStatusCodes.BAD_REQUEST,
                    MimeTypes.TEXT_PLAIN,
                    "Parameter " + QueryParameters.NEW_PARTITIONS.name() + " error: " + newPartitions.result().error());
            return;
        }

        Optional<Stream> optionalStream;
        try {
            optionalStream = streamRepository.read(streamName.get());
        } catch (CuratorException | DeserializationException ex) {
            LOGGER.error("Read stream failed with exception", ex);
            request.complete(HttpStatusCodes.INTERNAL_SERVER_ERROR);
            return;
        }
        if(!optionalStream.isPresent()) {
            request.complete(HttpStatusCodes.NOT_FOUND);
            return;
        }
        Stream stream = optionalStream.get();

        if (newPartitions.get() <= stream.getPartitions()) {
            request.complete(HttpStatusCodes.CONFLICT);
            return;
        }

        stream.setPartitions(newPartitions.get());

        TaskFuture taskFuture =
                taskQueue.submit(
                        new StreamTask(stream, StreamTaskType.INCREASE_PARTITIONS),
                        stream.getName(),
                        10_000L,//TODO: Move to Properties or add timeout query param
                        TimeUnit.MILLISECONDS);
        HttpAsyncApiHelper.awaitAndComplete(taskFuture, request);
    }
}
