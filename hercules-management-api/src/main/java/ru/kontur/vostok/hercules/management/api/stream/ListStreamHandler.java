package ru.kontur.vostok.hercules.management.api.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.undertow.util.ExchangeUtil;
import ru.kontur.vostok.hercules.undertow.util.ResponseUtil;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Gregory Koshelev
 */
public class ListStreamHandler implements HttpHandler {
    private final StreamRepository repository;

    private final ObjectMapper mapper = new ObjectMapper();

    public ListStreamHandler(StreamRepository repository) {
        this.repository = repository;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        Optional<String> apiKey = ExchangeUtil.extractHeaderValue(exchange, "apiKey");
        if (!apiKey.isPresent()) {
            ResponseUtil.unauthorized(exchange);
            return;
        }

        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        List<String> list = repository.list().stream()
                .sorted()
                .collect(Collectors.toList());
        exchange.getResponseSender().send(mapper.writeValueAsString(list));
    }
}
