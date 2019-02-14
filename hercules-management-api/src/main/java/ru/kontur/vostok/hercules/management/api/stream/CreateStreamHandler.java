package ru.kontur.vostok.hercules.management.api.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.auth.AuthResult;
import ru.kontur.vostok.hercules.meta.curator.result.CreationResult;
import ru.kontur.vostok.hercules.meta.stream.Stream;
import ru.kontur.vostok.hercules.meta.stream.validation.StreamValidators;
import ru.kontur.vostok.hercules.meta.task.stream.StreamTask;
import ru.kontur.vostok.hercules.meta.task.stream.StreamTaskRepository;
import ru.kontur.vostok.hercules.meta.task.stream.StreamTaskType;
import ru.kontur.vostok.hercules.undertow.util.ExchangeUtil;
import ru.kontur.vostok.hercules.undertow.util.ResponseUtil;
import ru.kontur.vostok.hercules.util.validation.Validator;

import java.io.IOException;
import java.util.Optional;

/**
 * @author Gregory Koshelev
 */
public class CreateStreamHandler implements HttpHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateStreamHandler.class);
    private static final Validator<Stream> STREAM_VALIDATOR = StreamValidators.streamValidatorForHandler();

    private final AuthManager authManager;
    private final StreamTaskRepository repository;

    private final ObjectReader deserializer;

    public CreateStreamHandler(AuthManager authManager, StreamTaskRepository repository) {
        this.authManager = authManager;
        this.repository = repository;

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

                Optional<String> streamError = STREAM_VALIDATOR.validate(stream);
                if(streamError.isPresent()) {
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

                CreationResult creationResult =
                        repository.create(new StreamTask(stream, StreamTaskType.CREATE), stream.getName());
                if (!creationResult.isSuccess()) {
                    if (creationResult.getStatus() == CreationResult.Status.ALREADY_EXIST) {
                        ResponseUtil.conflict(exch);
                    } else {
                        ResponseUtil.internalServerError(exch);
                    }
                    return;
                }

                //TODO: Wait for result if needed
                ResponseUtil.ok(exch);
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
