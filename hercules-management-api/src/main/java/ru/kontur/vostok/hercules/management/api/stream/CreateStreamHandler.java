package ru.kontur.vostok.hercules.management.api.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.auth.AuthResult;
import ru.kontur.vostok.hercules.management.api.task.KafkaTaskQueue;
import ru.kontur.vostok.hercules.meta.curator.CreationResult;
import ru.kontur.vostok.hercules.meta.stream.Stream;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.meta.stream.validation.Validation;
import ru.kontur.vostok.hercules.undertow.util.ExchangeUtil;
import ru.kontur.vostok.hercules.undertow.util.ResponseUtil;
import ru.kontur.vostok.hercules.util.functional.Result;

import java.io.IOException;
import java.util.Optional;

/**
 * @author Gregory Koshelev
 */
public class CreateStreamHandler implements HttpHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateStreamHandler.class);

    private final AuthManager authManager;
    private final StreamRepository repository;
    private final KafkaTaskQueue kafkaTaskQueue;

    private final ObjectReader deserializer;

    public CreateStreamHandler(AuthManager authManager, StreamRepository repository, KafkaTaskQueue kafkaTaskQueue) {
        this.authManager = authManager;
        this.repository = repository;
        this.kafkaTaskQueue = kafkaTaskQueue;

        ObjectMapper objectMapper = new ObjectMapper();
        this.deserializer = objectMapper.readerFor(Stream.class);
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        Optional<String> optionalApiKey = ExchangeUtil.extractHeaderValue(exchange, "apiKey");
        if (!optionalApiKey.isPresent()) {
            ResponseUtil.unauthorized(exchange);
            return;
        }

        final String apiKey = optionalApiKey.get();
        exchange.getRequestReceiver().receiveFullBytes((exch, bytes) -> {
            try {
                Stream stream = deserializer.readValue(bytes);
                Result result = Validation.check(stream);
                if(!result.isOk()) {
                    ResponseUtil.badRequest(exch);
                    return;
                }

                AuthResult authResult = authManager.authManage(apiKey, stream.getName());
                if (!authResult.isSuccess()) {
                    if (authResult.isUnknown()) {
                        ResponseUtil.unauthorized(exch);
                        return;
                    }
                    ResponseUtil.forbidden(exch);
                    return;
                }
                //TODO: Auth sources if needed

                CreationResult creationResult = repository.create(stream);
                if (!creationResult.isSuccess()) {
                    if (creationResult.getStatus() == CreationResult.Status.ALREADY_EXIST) {
                        ResponseUtil.conflict(exch);
                    } else {
                        ResponseUtil.internalServerError(exch);
                    }
                    return;
                }

                //TODO: Topic creation may fail after successful meta creation (no atomicity at all).
                kafkaTaskQueue.createTopic(stream.getName(), stream.getPartitions(), stream.getTtl());
            } catch (IOException e) {
                LOGGER.error("Error on processing request", e);
                ResponseUtil.badRequest(exch);
                return;
            } catch (Exception e) {
                LOGGER.error("Error on processing request", e);
                ResponseUtil.internalServerError(exch);
                return;
            }
        }, (exch, exception) -> {
            ResponseUtil.badRequest(exch);
            return;
        });
    }
}
