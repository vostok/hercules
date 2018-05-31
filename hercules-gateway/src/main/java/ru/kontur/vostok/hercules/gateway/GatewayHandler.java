package ru.kontur.vostok.hercules.gateway;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import ru.kontur.vostok.hercules.auth.Action;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.auth.AuthResult;
import ru.kontur.vostok.hercules.meta.stream.BaseStream;
import ru.kontur.vostok.hercules.meta.stream.Stream;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * @author Gregory Koshelev
 */
public abstract class GatewayHandler implements HttpHandler {
    private final AuthManager authManager;
    protected final EventSender eventSender;
    private final StreamRepository streamRepository;

    protected GatewayHandler(AuthManager authManager, EventSender eventSender, StreamRepository streamRepository) {
        this.authManager = authManager;
        this.eventSender = eventSender;
        this.streamRepository = streamRepository;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        Optional<String> optionalStream = ExchangeUtil.extractQueryParam(exchange, "stream");
        if (!optionalStream.isPresent()) {
            exchange.setStatusCode(400);
            exchange.endExchange();
            return;
        }
        String stream = optionalStream.get();
        //TODO: stream name validation

        if (!auth(exchange, stream)) {
            return;
        }

        // Check content length
        int contentLength = ExchangeUtil.extractContentLength(exchange);
        if (contentLength < 0 || contentLength > (1 << 21)) {
            exchange.setStatusCode(400);
            exchange.endExchange();
            return;
        }

        Optional<Stream> optionalBaseStream = streamRepository.read(stream);
        if (!optionalBaseStream.isPresent()) {
            exchange.setStatusCode(404);
            exchange.endExchange();
            return;
        }
        Stream baseStream = optionalBaseStream.get();
        if (!(baseStream instanceof BaseStream)) {
            exchange.setStatusCode(400);
            exchange.endExchange();
            return;
        }

        String[] shardingKey = baseStream.getShardingKey();
        int partitions = baseStream.getPartitions();
        String topic = baseStream.getName();
        Set<String> tags = new HashSet<>(shardingKey.length);
        tags.addAll(Arrays.asList(shardingKey));

        send(exchange, topic, tags, partitions, shardingKey);
    }

    protected abstract void send(HttpServerExchange exchange, String topic, Set<String> tags, int partitions, String[] shardingKey);

    protected EventSender getEventSender() {
        return eventSender;
    }

    private boolean auth(HttpServerExchange exchange, String stream) {
        Optional<String> optionalApiKey = ExchangeUtil.extractHeaderValue(exchange, "apiKey");
        if (!optionalApiKey.isPresent()) {
            exchange.setStatusCode(401);
            exchange.endExchange();
            return false;
        }

        AuthResult authResult = authManager.authStream(optionalApiKey.get(), stream, Action.WRITE);

        if (authResult.isSuccess()) {
            return true;
        }

        if (authResult.isUnknown()) {
            exchange.setStatusCode(401);
            exchange.endExchange();
            return false;
        }

        exchange.setStatusCode(403);
        exchange.endExchange();
        return false;
    }
}
