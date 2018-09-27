package ru.kontur.vostok.hercules.management.api.timeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.auth.AuthResult;
import ru.kontur.vostok.hercules.management.api.task.CassandraTaskQueue;
import ru.kontur.vostok.hercules.meta.curator.CreationResult;
import ru.kontur.vostok.hercules.meta.timeline.Timeline;
import ru.kontur.vostok.hercules.meta.timeline.TimelineRepository;
import ru.kontur.vostok.hercules.undertow.util.ExchangeUtil;
import ru.kontur.vostok.hercules.undertow.util.ResponseUtil;

import java.io.IOException;
import java.util.Optional;

/**
 * @author Gregory Koshelev
 */
public class CreateTimelineHandler implements HttpHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateTimelineHandler.class);

    private final AuthManager authManager;
    private final TimelineRepository repository;
    private final CassandraTaskQueue cassandraTaskQueue;

    private final ObjectReader deserializer;

    public CreateTimelineHandler(AuthManager authManager, TimelineRepository repository, CassandraTaskQueue cassandraTaskQueue) {
        this.authManager = authManager;
        this.repository = repository;
        this.cassandraTaskQueue = cassandraTaskQueue;

        ObjectMapper objectMapper = new ObjectMapper();
        this.deserializer = objectMapper.readerFor(Timeline.class);
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
                //TODO: Auth sources

                CreationResult creationResult = repository.create(timeline);
                if (!creationResult.isSuccess()) {
                    if (creationResult.getStatus() == CreationResult.Status.ALREADY_EXIST) {
                        ResponseUtil.conflict(exch);
                    } else {
                        ResponseUtil.internalServerError(exch);
                    }
                    return;
                }

                //TODO: Table creation may fail after successful meta creation (no atomicity at all).
                cassandraTaskQueue.createTable(timeline.getName());
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
