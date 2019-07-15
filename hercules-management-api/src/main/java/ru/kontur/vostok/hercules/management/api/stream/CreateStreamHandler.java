package ru.kontur.vostok.hercules.management.api.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.auth.AuthResult;
import ru.kontur.vostok.hercules.meta.stream.DerivedStream;
import ru.kontur.vostok.hercules.meta.stream.Stream;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.meta.stream.validation.StreamValidators;
import ru.kontur.vostok.hercules.meta.task.TaskFuture;
import ru.kontur.vostok.hercules.meta.task.TaskQueue;
import ru.kontur.vostok.hercules.meta.task.stream.StreamTask;
import ru.kontur.vostok.hercules.meta.task.stream.StreamTaskType;
import ru.kontur.vostok.hercules.undertow.util.ExchangeUtil;
import ru.kontur.vostok.hercules.undertow.util.ResponseUtil;
import ru.kontur.vostok.hercules.util.validation.ValidationResult;
import ru.kontur.vostok.hercules.util.validation.Validator;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class CreateStreamHandler implements HttpHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateStreamHandler.class);
    private static final Validator<Stream> STREAM_VALIDATOR = StreamValidators.streamValidatorForHandler();

    private final AuthManager authManager;
    private final TaskQueue<StreamTask> taskQueue;
    private final StreamRepository streamRepository;

    private final ObjectReader deserializer;

    public CreateStreamHandler(AuthManager authManager, TaskQueue<StreamTask> taskQueue, StreamRepository streamRepository) {
        this.authManager = authManager;
        this.taskQueue = taskQueue;
        this.streamRepository = streamRepository;

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

        Optional<Integer> optionalContentLength = ExchangeUtil.extractContentLength(exchange);
        if (!optionalContentLength.isPresent()) {
            ResponseUtil.lengthRequired(exchange);
            return;
        }

        final String apiKey = optionalApiKey.get();
        exchange.getRequestReceiver().receiveFullBytes((exch, bytes) -> {
            try {
                Stream stream = deserializer.readValue(bytes);

                if (stream.getShardingKey() == null) {
                    stream.setShardingKey(new String[0]);
                }

                ValidationResult validationResult = STREAM_VALIDATOR.validate(stream);
                if (validationResult.isError()) {
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

                if (streamRepository.exists(stream.getName())) {
                    ResponseUtil.conflict(exch);
                    return;
                }

                if (stream instanceof DerivedStream) {// Auth source streams for DerivedStream
                    String[] streams = ((DerivedStream) stream).getStreams();
                    if (streams == null || streams.length == 0) {
                        ResponseUtil.badRequest(exch);
                        return;
                    }
                    for (String sourceStream : streams) {
                        authResult = authManager.authRead(apiKey, sourceStream);
                        if (!authResult.isSuccess()) {
                            ResponseUtil.forbidden(exch);
                            return;
                        }
                    }
                }

                TaskFuture taskFuture =
                        taskQueue.submit(
                                new StreamTask(stream, StreamTaskType.CREATE),
                                stream.getName(),
                                10_000L,//TODO: Move to properties
                                TimeUnit.MILLISECONDS);
                if (taskFuture.isFailed()) {
                    ResponseUtil.internalServerError(exch);
                    return;
                }

                if (!ExchangeUtil.extractQueryParam(exch, "async").isPresent()) {
                    taskFuture.await();
                    if (taskFuture.isDone()) {
                        ResponseUtil.ok(exch);
                        return;
                    }
                    ResponseUtil.requestTimeout(exch);
                    return;
                }
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
