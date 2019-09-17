package ru.kontur.vostok.hercules.management.api.timeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.auth.AuthProvider;
import ru.kontur.vostok.hercules.auth.AuthResult;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.http.handler.HttpHandler;
import ru.kontur.vostok.hercules.management.api.HttpAsyncApiHelper;
import ru.kontur.vostok.hercules.meta.task.TaskFuture;
import ru.kontur.vostok.hercules.meta.task.TaskQueue;
import ru.kontur.vostok.hercules.meta.task.timeline.TimelineTask;
import ru.kontur.vostok.hercules.meta.task.timeline.TimelineTaskType;
import ru.kontur.vostok.hercules.meta.timeline.Timeline;
import ru.kontur.vostok.hercules.meta.timeline.TimelineRepository;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class CreateTimelineHandler implements HttpHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateTimelineHandler.class);

    private final AuthProvider authProvider;
    private final TaskQueue<TimelineTask> taskQueue;
    private final TimelineRepository repository;

    private final ObjectReader deserializer;

    public CreateTimelineHandler(AuthProvider authProvider, TaskQueue<TimelineTask> taskQueue, TimelineRepository repository) {
        this.authProvider = authProvider;
        this.taskQueue = taskQueue;
        this.repository = repository;

        ObjectMapper objectMapper = new ObjectMapper();
        this.deserializer = objectMapper.readerFor(Timeline.class);
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
                Timeline timeline = deserializer.readValue(bytes);

                AuthResult authResult = authProvider.authManage(r, timeline.getName());
                if (!authResult.isSuccess()) {
                    if (authResult.isUnknown()) {
                        r.complete(HttpStatusCodes.UNAUTHORIZED);
                        return;
                    }
                    r.complete(HttpStatusCodes.FORBIDDEN);
                    return;
                }

                if (repository.exists(timeline.getName())) {
                    r.complete(HttpStatusCodes.CONFLICT);
                    return;
                }

                String[] streams = timeline.getStreams();
                if (streams == null || streams.length == 0) {
                    r.complete(HttpStatusCodes.BAD_REQUEST);
                    return;
                }
                for (String stream : streams) {
                    authResult = authProvider.authRead(r, stream);
                    if (!authResult.isSuccess()) {
                        r.complete(HttpStatusCodes.FORBIDDEN);
                        return;
                    }
                }

                TaskFuture taskFuture =
                        taskQueue.submit(
                                new TimelineTask(timeline, TimelineTaskType.CREATE),
                                timeline.getName(),
                                10_000L,//TODO: Move to properties
                                TimeUnit.MILLISECONDS);
                HttpAsyncApiHelper.awaitAndComplete(taskFuture, request);
            } catch (IOException ex) {
                LOGGER.warn("Error on processing request", ex);
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
