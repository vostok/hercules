package ru.kontur.vostok.hercules.management.api.stream;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.management.api.task.KafkaTaskQueue;
import ru.kontur.vostok.hercules.meta.curator.DeletionResult;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.undertow.util.ExchangeUtil;
import ru.kontur.vostok.hercules.undertow.util.ResponseUtil;

import java.util.Optional;

/**
 * @author Gregory Koshelev
 */
public class DeleteStreamHandler implements HttpHandler {
    private final AuthManager authManager;
    private final StreamRepository repository;
    private final KafkaTaskQueue kafkaTaskQueue;

    public DeleteStreamHandler(AuthManager authManager, StreamRepository repository, KafkaTaskQueue kafkaTaskQueue) {
        this.authManager = authManager;
        this.repository = repository;
        this.kafkaTaskQueue = kafkaTaskQueue;
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

        //TODO: auth

        kafkaTaskQueue.deleteTopic(stream);

        //TODO: Meta deletion may fail after successful topic deletion (no atomicity at all).
        DeletionResult deletionResult = repository.delete(stream);
        if (!deletionResult.isSuccess()) {
            switch (deletionResult.getStatus()) {
                case NOT_EXIST:
                    ResponseUtil.notFound(exchange);
                    return;
                case UNKNOWN:
                    ResponseUtil.internalServerError(exchange);
                    return;
            }
        }

        ResponseUtil.ok(exchange);
    }
}
