package ru.kontur.vostok.hercules.management.api.stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.auth.AuthProvider;
import ru.kontur.vostok.hercules.auth.AuthResult;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.http.handler.HttpHandler;
import ru.kontur.vostok.hercules.http.query.QueryUtil;
import ru.kontur.vostok.hercules.management.api.HttpAsyncApiHelper;
import ru.kontur.vostok.hercules.management.api.QueryParameters;
import ru.kontur.vostok.hercules.meta.serialization.DeserializationException;
import ru.kontur.vostok.hercules.meta.serialization.Deserializer;
import ru.kontur.vostok.hercules.meta.stream.DerivedStream;
import ru.kontur.vostok.hercules.meta.stream.Stream;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.meta.stream.validation.StreamValidators;
import ru.kontur.vostok.hercules.meta.task.TaskFuture;
import ru.kontur.vostok.hercules.meta.task.TaskQueue;
import ru.kontur.vostok.hercules.meta.task.stream.StreamTask;
import ru.kontur.vostok.hercules.meta.task.stream.StreamTaskType;
import ru.kontur.vostok.hercules.util.validation.ValidationResult;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class CreateStreamHandler implements HttpHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateStreamHandler.class);

    private final AuthProvider authProvider;
    private final TaskQueue<StreamTask> taskQueue;
    private final StreamRepository streamRepository;

    private final Deserializer deserializer;

    public CreateStreamHandler(AuthProvider authProvider, TaskQueue<StreamTask> taskQueue, StreamRepository streamRepository) {
        this.authProvider = authProvider;
        this.taskQueue = taskQueue;
        this.streamRepository = streamRepository;

        this.deserializer = Deserializer.forClass(Stream.class);
    }

    @Override
    public void handle(HttpServerRequest request) {
        Optional<Integer> optionalContentLength = request.getContentLength();
        if (!optionalContentLength.isPresent()) {
            request.complete(HttpStatusCodes.LENGTH_REQUIRED);
            return;
        }

        request.readBodyAsync((r, bytes) -> {
            try {
                Stream stream = deserializer.deserialize(bytes);

                if (stream.getShardingKey() == null) {
                    stream.setShardingKey(new String[0]);
                }

                ValidationResult validationResult = StreamValidators.STREAM_VALIDATOR.validate(stream);
                if (validationResult.isError()) {
                    LOGGER.warn(validationResult.error());
                    r.complete(HttpStatusCodes.BAD_REQUEST);
                    return;
                }

                AuthResult authResult = authProvider.authManage(r, stream.getName());
                if (!authResult.isSuccess()) {
                    if (authResult.isUnknown()) {
                        r.complete(HttpStatusCodes.UNAUTHORIZED);
                        return;
                    }
                    r.complete(HttpStatusCodes.FORBIDDEN);
                    return;
                }

                if (streamRepository.exists(stream.getName())) {
                    r.complete(HttpStatusCodes.CONFLICT);
                    return;
                }

                if (stream instanceof DerivedStream) {// Auth source streams for DerivedStream
                    String[] streams = ((DerivedStream) stream).getStreams();
                    if (streams == null || streams.length == 0) {
                        r.complete(HttpStatusCodes.BAD_REQUEST);
                        return;
                    }
                    for (String sourceStream : streams) {
                        authResult = authProvider.authRead(r, sourceStream);
                        if (!authResult.isSuccess()) {
                            r.complete(HttpStatusCodes.FORBIDDEN);
                            return;
                        }
                    }
                }

                TaskFuture taskFuture =
                        taskQueue.submit(
                                new StreamTask(stream, StreamTaskType.CREATE),
                                stream.getName(),
                                QueryUtil.get(QueryParameters.TIMEOUT_MS, request).get(),
                                TimeUnit.MILLISECONDS);
                HttpAsyncApiHelper.awaitAndComplete(taskFuture, r);
            } catch (DeserializationException ex) {
                LOGGER.warn("Error on entity deserialization", ex);
                r.complete(HttpStatusCodes.BAD_REQUEST);
                return;
            } catch (Exception ex) {
                LOGGER.error("Error on processing request", ex);
                r.complete(HttpStatusCodes.INTERNAL_SERVER_ERROR);
                return;
            }
        }, (r, exception) -> {
            LOGGER.error("Error on processing request", exception);
            r.complete(HttpStatusCodes.BAD_REQUEST);
            return;
        });
    }
}
