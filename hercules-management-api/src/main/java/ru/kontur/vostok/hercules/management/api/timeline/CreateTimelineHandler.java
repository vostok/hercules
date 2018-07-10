package ru.kontur.vostok.hercules.management.api.timeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import ru.kontur.vostok.hercules.auth.Action;
import ru.kontur.vostok.hercules.auth.AuthManager;
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
    private final AuthManager authManager;
    private final TimelineRepository repository;

    private final ObjectReader deserializer;

    public CreateTimelineHandler(AuthManager authManager, TimelineRepository repository) {
        this.authManager = authManager;
        this.repository = repository;

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

                authManager.auth(apiKey, timeline.getName(), Action.MANAGE);
                //TODO: Auth sources

                repository.create(timeline);
                //TODO: create table too
            } catch (IOException e) {
                e.printStackTrace();
                ResponseUtil.badRequest(exch);
                return;
            } catch (Exception e) {
                e.printStackTrace();
                ResponseUtil.internalServerError(exch);
                return;
            }
        }, (exch, exception) -> {
            ResponseUtil.badRequest(exch);
            return;
        });
    }
}
