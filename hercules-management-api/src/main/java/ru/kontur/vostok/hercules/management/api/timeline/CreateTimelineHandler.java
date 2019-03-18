package ru.kontur.vostok.hercules.management.api.timeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.auth.AuthResult;
import ru.kontur.vostok.hercules.meta.task.TaskFuture;
import ru.kontur.vostok.hercules.meta.task.TaskQueue;
import ru.kontur.vostok.hercules.meta.task.timeline.TimelineTask;
import ru.kontur.vostok.hercules.meta.task.timeline.TimelineTaskType;
import ru.kontur.vostok.hercules.meta.timeline.Timeline;
import ru.kontur.vostok.hercules.meta.timeline.TimelineRepository;
import ru.kontur.vostok.hercules.undertow.util.ExchangeUtil;
import ru.kontur.vostok.hercules.undertow.util.ResponseUtil;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class CreateTimelineHandler implements HttpHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateTimelineHandler.class);

    private final AuthManager authManager;
    private final TaskQueue<TimelineTask> taskQueue;
    private final TimelineRepository repository;

    private final ObjectReader deserializer;

    public CreateTimelineHandler(AuthManager authManager, TaskQueue<TimelineTask> taskQueue, TimelineRepository repository) {
        this.authManager = authManager;
        this.taskQueue = taskQueue;
        this.repository = repository;

        ObjectMapper objectMapper = new ObjectMapper();
        this.deserializer = objectMapper.readerFor(Timeline.class);
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
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
        int contentLength = optionalContentLength.get();
        if (contentLength < 0) {
            ResponseUtil.badRequest(exchange);
            return;
        }

        final String apiKey = optionalApiKey.get();
        exchange.getRequestReceiver().receiveFullBytes((exch, bytes) -> {
            try {
                Timeline timeline = deserializer.readValue(bytes);

                AuthResult authResult = authManager.authManage(apiKey, timeline.getName());
                if (!authResult.isSuccess()) {
                    if (authResult.isUnknown()) {
                        ResponseUtil.unauthorized(exch);
                        return;
                    }
                    ResponseUtil.forbidden(exch);
                    return;
                }

                if (repository.exists(timeline.getName())) {
                    ResponseUtil.conflict(exch);
                    return;
                }

                String[] streams = timeline.getStreams();
                if (streams == null || streams.length == 0) {
                    ResponseUtil.badRequest(exch);
                    return;
                }
                for (String stream : streams) {
                    authResult = authManager.authRead(apiKey, stream);
                    if (!authResult.isSuccess()) {
                        ResponseUtil.forbidden(exch);
                        return;
                    }
                }

                TaskFuture taskFuture =
                        taskQueue.submit(
                                new TimelineTask(timeline, TimelineTaskType.CREATE),
                                timeline.getName(),
                                10_000L,//TODO: Move to properties
                                TimeUnit.MILLISECONDS);

                if (taskFuture.isExpired()) {
                    ResponseUtil.requestTimeout(exch);
                    return;
                }

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
                    ResponseUtil.internalServerError(exch);
                    return;
                }
                ResponseUtil.ok(exch);
            } catch (IOException e) {
                LOGGER.error("Error on performing request", e);
                ResponseUtil.badRequest(exch);
                return;
            } catch (Exception e) {
                LOGGER.error("Error on performing request", e);
                ResponseUtil.internalServerError(exch);
                return;
            }
        }, (exch, exception) -> {
            ResponseUtil.badRequest(exch);
            return;
        });
    }
}
