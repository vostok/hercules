package ru.kontur.vostok.hercules.management.api.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import ru.kontur.vostok.hercules.meta.stream.Stream;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.undertow.util.ExchangeUtil;
import ru.kontur.vostok.hercules.undertow.util.ResponseUtil;

import java.util.Optional;

/**
 * @author Vladimir Tsypaev
 */
public class InfoStreamHandler implements HttpHandler {

    private final ObjectMapper mapper = new ObjectMapper();
    private final StreamRepository repository;

    public InfoStreamHandler(StreamRepository repository) {
        this.repository = repository;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        Optional<String> apiKey = ExchangeUtil.extractHeaderValue(exchange, "apiKey");
        if (!apiKey.isPresent()) {
            ResponseUtil.unauthorized(exchange);
            return;
        }

        Optional<String> streamName = ExchangeUtil.extractQueryParam(exchange, "stream");
        if (!streamName.isPresent()) {
            ResponseUtil.badRequest(exchange);
            return;
        }

        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");

        Optional<Stream> stream = repository.read(streamName.get());
        if (!stream.isPresent()) {
            ResponseUtil.notFound(exchange);
            return;
        }

        exchange.getResponseSender().send(mapper.writeValueAsString(stream.get()));
    }
}
