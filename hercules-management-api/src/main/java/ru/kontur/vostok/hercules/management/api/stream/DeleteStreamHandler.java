package ru.kontur.vostok.hercules.management.api.stream;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.auth.AuthResult;
import ru.kontur.vostok.hercules.meta.curator.result.CreationResult;
import ru.kontur.vostok.hercules.meta.stream.BaseStream;
import ru.kontur.vostok.hercules.meta.task.stream.StreamTask;
import ru.kontur.vostok.hercules.meta.task.stream.StreamTaskRepository;
import ru.kontur.vostok.hercules.meta.task.stream.StreamTaskType;
import ru.kontur.vostok.hercules.undertow.util.ExchangeUtil;
import ru.kontur.vostok.hercules.undertow.util.ResponseUtil;

import java.util.Optional;

/**
 * @author Gregory Koshelev
 */
public class DeleteStreamHandler implements HttpHandler {
    private final AuthManager authManager;
    private final StreamTaskRepository repository;

    public DeleteStreamHandler(AuthManager authManager, StreamTaskRepository repository) {
        this.authManager = authManager;
        this.repository = repository;
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

        BaseStream stub = new BaseStream();
        stub.setName(stream);

        CreationResult creationResult =
                repository.create(new StreamTask(stub, StreamTaskType.DELETE), stub.getName());
        if (!creationResult.isSuccess()) {
            ResponseUtil.internalServerError(exchange);
            return;
        }

        //TODO: Wait for result if needed
        ResponseUtil.ok(exchange);
    }
}
