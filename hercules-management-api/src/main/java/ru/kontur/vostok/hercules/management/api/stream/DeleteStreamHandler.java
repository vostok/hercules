package ru.kontur.vostok.hercules.management.api.stream;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.auth.AuthResult;
import ru.kontur.vostok.hercules.meta.stream.BaseStream;
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
public class DeleteStreamHandler implements HttpHandler {
    private final AuthManager authManager;
    private final TaskQueue<StreamTask> taskQueue;
    private final StreamRepository streamRepository;

    public DeleteStreamHandler(AuthManager authManager, TaskQueue<StreamTask> taskQueue, StreamRepository streamRepository) {
        this.authManager = authManager;
        this.taskQueue = taskQueue;
        this.streamRepository = streamRepository;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        Optional<String> apiKey = ExchangeUtil.extractHeaderValue(exchange, "apiKey");
        if (!apiKey.isPresent()) {
            ResponseUtil.unauthorized(exchange);
            return;
        }

        Optional<String> optionalStream = ExchangeUtil.extractQueryParam(exchange, "stream");
        if (!optionalStream.isPresent()) {
            ResponseUtil.badRequest(exchange);
            return;
        }
        String stream = optionalStream.get();

        AuthResult authResult = authManager.authManage(apiKey.get(), stream);
        if (!authResult.isSuccess()) {
            if (authResult.isUnknown()) {
                ResponseUtil.unauthorized(exchange);
                return;
            }
            ResponseUtil.forbidden(exchange);
            return;
        }

        if (!streamRepository.exists(stream)) {
            ResponseUtil.notFound(exchange);
            return;
        }

        BaseStream stub = new BaseStream();
        stub.setName(stream);

        TaskFuture taskFuture =
                taskQueue.submit(
                        new StreamTask(stub, StreamTaskType.DELETE),
                        stub.getName(),
                10_000L,//TODO: Move to Properties
                TimeUnit.MILLISECONDS);

        if (taskFuture.isExpired()) {
            ResponseUtil.requestTimeout(exchange);
            return;
        }

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
