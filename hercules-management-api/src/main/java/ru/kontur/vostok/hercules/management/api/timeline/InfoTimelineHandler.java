package ru.kontur.vostok.hercules.management.api.timeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import ru.kontur.vostok.hercules.meta.timeline.Timeline;
import ru.kontur.vostok.hercules.meta.timeline.TimelineRepository;
import ru.kontur.vostok.hercules.undertow.util.ExchangeUtil;
import ru.kontur.vostok.hercules.undertow.util.ResponseUtil;

import java.util.Optional;

/**
 * @author Vladimir Tsypaev
 */
public class InfoTimelineHandler implements HttpHandler {

    private final ObjectMapper mapper = new ObjectMapper();
    private final TimelineRepository repository;

    public InfoTimelineHandler(TimelineRepository repository) {
        this.repository = repository;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        Optional<String> apiKey = ExchangeUtil.extractHeaderValue(exchange, "apiKey");
        if (!apiKey.isPresent()) {
            ResponseUtil.unauthorized(exchange);
            return;
        }

        Optional<String> timelineName = ExchangeUtil.extractQueryParam(exchange, "timeline");
        if (!timelineName.isPresent()) {
            ResponseUtil.badRequest(exchange);
            return;
        }

        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");

        Optional<Timeline> timeline = repository.read(timelineName.get());
        if (!timeline.isPresent()) {
            ResponseUtil.notFound(exchange);
            return;
        }

        exchange.getResponseSender().send(mapper.writeValueAsString(timeline.get()));
    }
}
