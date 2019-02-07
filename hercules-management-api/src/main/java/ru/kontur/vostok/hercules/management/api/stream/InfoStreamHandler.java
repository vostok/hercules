package ru.kontur.vostok.hercules.management.api.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.auth.AuthResult;
import ru.kontur.vostok.hercules.meta.stream.Stream;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.undertow.util.ExchangeUtil;
import ru.kontur.vostok.hercules.undertow.util.ResponseUtil;

import java.util.Optional;

/**
 * @author Vladimir Tsypaev
 */
public class InfoStreamHandler implements HttpHandler {

    private final AuthManager authManager;
    private final ObjectMapper mapper = new ObjectMapper();
    private final StreamRepository repository;

    public InfoStreamHandler(StreamRepository repository, AuthManager authManager) {
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

        Optional<String> optionalStreamName = ExchangeUtil.extractQueryParam(exchange, "stream");
        if (!optionalStreamName.isPresent()) {
            ResponseUtil.badRequest(exchange);
            return;
        }

        String apiKeyName = optionalApiKey.get();
        String streamName = optionalStreamName.get();

        AuthResult authResult = authManager.authManage(apiKeyName, streamName);

        if (!authResult.isSuccess()) {
            if (authResult.isUnknown()) {
                ResponseUtil.unauthorized(exchange);
                return;
            }
            ResponseUtil.forbidden(exchange);
            return;
        }

        Optional<Stream> stream = repository.read(streamName);
        if (!stream.isPresent()) {
            ResponseUtil.notFound(exchange);
            return;
        }

        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        exchange.getResponseSender().send(mapper.writeValueAsString(stream.get()));
    }
}
