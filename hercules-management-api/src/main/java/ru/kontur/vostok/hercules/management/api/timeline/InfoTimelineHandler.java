package ru.kontur.vostok.hercules.management.api.timeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.auth.AuthResult;
import ru.kontur.vostok.hercules.meta.timeline.Timeline;
import ru.kontur.vostok.hercules.meta.timeline.TimelineRepository;
import ru.kontur.vostok.hercules.undertow.util.ExchangeUtil;
import ru.kontur.vostok.hercules.undertow.util.ResponseUtil;

import java.util.Optional;

/**
 * @author Vladimir Tsypaev
 */
public class InfoTimelineHandler implements HttpHandler {

    private final AuthManager authManager;
    private final ObjectMapper mapper = new ObjectMapper();
    private final TimelineRepository repository;

    public InfoTimelineHandler(TimelineRepository repository, AuthManager authManager) {
        this.repository = repository;
        this.authManager = authManager;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        Optional<String> optionalApiKey = ExchangeUtil.extractHeaderValue(exchange, "apiKey");
        if (!optionalApiKey.isPresent()) {
            ResponseUtil.unauthorized(exchange);
            return;
        }

        Optional<String> optionalTimeline = ExchangeUtil.extractQueryParam(exchange, "timeline");
        if (!optionalTimeline.isPresent()) {
            ResponseUtil.badRequest(exchange);
            return;
        }

        String apiKeyName = optionalApiKey.get();
        String timelineName = optionalTimeline.get();

        AuthResult authResult = authManager.authManage(apiKeyName, timelineName);

        if (!authResult.isSuccess()) {
            if (authResult.isUnknown()) {
                ResponseUtil.unauthorized(exchange);
                return;
            }
            ResponseUtil.forbidden(exchange);
            return;
        }

        Optional<Timeline> timeline = repository.read(timelineName);
        if (!timeline.isPresent()) {
            ResponseUtil.notFound(exchange);
            return;
        }

        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        exchange.getResponseSender().send(mapper.writeValueAsString(timeline.get()));
    }
}
