package ru.kontur.vostok.hercules.management.api.stream;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.auth.AuthResult;
import ru.kontur.vostok.hercules.curator.exception.CuratorInternalException;
import ru.kontur.vostok.hercules.curator.exception.CuratorUnknownException;
import ru.kontur.vostok.hercules.meta.serialization.DeserializationException;
import ru.kontur.vostok.hercules.meta.stream.Stream;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.meta.task.TaskFuture;
import ru.kontur.vostok.hercules.meta.task.TaskQueue;
import ru.kontur.vostok.hercules.meta.task.stream.StreamTask;
import ru.kontur.vostok.hercules.meta.task.stream.StreamTaskType;
import ru.kontur.vostok.hercules.undertow.util.ExchangeUtil;
import ru.kontur.vostok.hercules.undertow.util.ResponseUtil;

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
    public void handleRequest(HttpServerExchange exchange) {
        Optional<String> optionalApiKey = ExchangeUtil.extractHeaderValue(exchange, "apiKey");
        if (!optionalApiKey.isPresent()) {
            ResponseUtil.unauthorized(exchange);
            return;
        }

        final String apiKey = optionalApiKey.get();

        Optional<String> optionalStreamName = ExchangeUtil.extractQueryParam(exchange, "stream");
        if (!optionalStreamName.isPresent()) {
            ResponseUtil.badRequest(exchange);
            return;
        }
        final String streamName = optionalStreamName.get();

        AuthResult authResult = authManager.authManage(apiKey, streamName);
        if (!authResult.isSuccess()) {
            if (authResult.isUnknown()) {
                ResponseUtil.unauthorized(exchange);
                return;
            }
            ResponseUtil.forbidden(exchange);
            return;
        }

        Optional<Integer> optionalNewPartitions = ExchangeUtil.extractIntegerQueryParam(exchange, "newPartitions");
        if (!optionalNewPartitions.isPresent()) {
            ResponseUtil.badRequest(exchange);
            return;
        }
        int newPartitions = optionalNewPartitions.get();
        if (newPartitions > 48) {//FIXME: proper validation is needed (without magic numbers ofc)
            ResponseUtil.badRequest(exchange);
            return;
        }

        Optional<Stream> optionalStream;
        try {
            optionalStream = streamRepository.read(streamName);
        } catch (CuratorUnknownException | CuratorInternalException | DeserializationException e) {
            ResponseUtil.internalServerError(exchange);
            return;
        }
        if(!optionalStream.isPresent()) {
            ResponseUtil.notFound(exchange);
            return;
        }
        Stream stream = optionalStream.get();

        if (newPartitions <= stream.getPartitions()) {
            ResponseUtil.conflict(exchange);
            return;
        }

        TaskFuture taskFuture =
                taskQueue.submit(
                        new StreamTask(stream, StreamTaskType.INCREASE_PARTITIONS),
                        stream.getName(),
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
