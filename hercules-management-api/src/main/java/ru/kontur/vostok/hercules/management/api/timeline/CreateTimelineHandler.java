package ru.kontur.vostok.hercules.management.api.timeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.auth.AuthProvider;
import ru.kontur.vostok.hercules.auth.AuthResult;
import ru.kontur.vostok.hercules.auth.AuthUtil;
import ru.kontur.vostok.hercules.http.ContentTypes;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.http.handler.HttpHandler;
import ru.kontur.vostok.hercules.http.query.QueryUtil;
import ru.kontur.vostok.hercules.management.api.HttpAsyncApiHelper;
import ru.kontur.vostok.hercules.management.api.QueryParameters;
import ru.kontur.vostok.hercules.meta.serialization.DeserializationException;
import ru.kontur.vostok.hercules.meta.serialization.Deserializer;
import ru.kontur.vostok.hercules.meta.task.TaskFuture;
import ru.kontur.vostok.hercules.meta.task.TaskQueue;
import ru.kontur.vostok.hercules.meta.task.timeline.TimelineTask;
import ru.kontur.vostok.hercules.meta.task.timeline.TimelineTaskType;
import ru.kontur.vostok.hercules.meta.timeline.Timeline;
import ru.kontur.vostok.hercules.meta.timeline.TimelineRepository;

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

    private final Deserializer deserializer;

    public CreateTimelineHandler(AuthProvider authProvider, TaskQueue<TimelineTask> taskQueue, TimelineRepository repository) {
        this.authProvider = authProvider;
        this.taskQueue = taskQueue;
        this.repository = repository;

        this.deserializer = Deserializer.forClass(Timeline.class);
    }

    @Override
    public void handle(HttpServerRequest request) {
        Optional<Integer> optionalContentLength = request.getContentLength();
        if (!optionalContentLength.isPresent()) {
            request.complete(HttpStatusCodes.LENGTH_REQUIRED, ContentTypes.TEXT_PLAIN_UTF_8,
                    "Content length must be specified");
            return;
        }

        request.readBodyAsync((r, bytes) -> {
            try {
                Timeline timeline = deserializer.deserialize(bytes);

                AuthResult authResult = authProvider.authManage(r, timeline.getName());
                if (AuthUtil.tryCompleteRequestIfUnsuccessfulAuth(request, authResult)) {
                    return;
                }

                //TODO: Validate timeline

                if (repository.exists(timeline.getName())) {
                    r.complete(HttpStatusCodes.CONFLICT, ContentTypes.TEXT_PLAIN_UTF_8,
                            "Timeline with a name " + timeline.getName() + " already exists");
                    return;
                }

                String[] streams = timeline.getStreams();
                if (streams == null || streams.length == 0) {
                    r.complete(HttpStatusCodes.BAD_REQUEST, ContentTypes.TEXT_PLAIN_UTF_8,
                            "Array of streams for timeline is absent");
                    return;
                }
                for (String stream : streams) {
                    authResult = authProvider.authRead(r, stream);
                    if (AuthUtil.tryCompleteRequestIfUnsuccessfulAuth(request, authResult)) {
                        return;
                    }
                }

                TaskFuture taskFuture =
                        taskQueue.submit(
                                new TimelineTask(timeline, TimelineTaskType.CREATE),
                                timeline.getName(),
                                QueryUtil.get(QueryParameters.TIMEOUT_MS, request).get(),
                                TimeUnit.MILLISECONDS);
                HttpAsyncApiHelper.awaitAndComplete(taskFuture, request);
            } catch (DeserializationException ex) {
                LOGGER.warn("Error on entity deserialization", ex);
                r.complete(HttpStatusCodes.BAD_REQUEST, ContentTypes.TEXT_PLAIN_UTF_8,
                        "Exception while trying to deserialize timeline");
                return;
            } catch (Exception ex) {
                LOGGER.error("Error on processing request", ex);
                r.complete(HttpStatusCodes.INTERNAL_SERVER_ERROR);
                return;
            }
        }, (r, exception) -> {
            LOGGER.error("Error on processing request", exception);
            r.complete(HttpStatusCodes.INTERNAL_SERVER_ERROR);
            return;
        });
    }
}
