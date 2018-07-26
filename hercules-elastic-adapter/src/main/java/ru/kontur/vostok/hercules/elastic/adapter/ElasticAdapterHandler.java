package ru.kontur.vostok.hercules.elastic.adapter;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import ru.kontur.vostok.hercules.elastic.adapter.util.ElasticAdapterUtil;
import ru.kontur.vostok.hercules.gateway.client.util.EventWriterUtil;
import ru.kontur.vostok.hercules.protocol.CommonConstants;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.encoder.EventBuilder;
import ru.kontur.vostok.hercules.undertow.util.ExchangeUtil;
import ru.kontur.vostok.hercules.undertow.util.ResponseUtil;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Http Handler for handling requests which are sent to elastic adapter.
 * Process logs. Convert json to Hercules protocol event format.
 *
 * @author Daniil Zhenikhov
 */
public class ElasticAdapterHandler implements HttpHandler {
    private final IndexResolver indexResolver = IndexResolverFactory.getInstance();
    private final Pattern authHeaderPattern = Pattern.compile("ELK ([a-zA-Z0-9\\-]+)");

    private final ElasticAdapterFunction handler;

    public ElasticAdapterHandler(ElasticAdapterFunction handler) {
        this.handler = handler;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        Optional<String> optionalIndex = ExchangeUtil.extractQueryParam(exchange, "index");
        if (!optionalIndex.isPresent()) {
            ResponseUtil.badRequest(exchange);
            return;
        }
        String index = optionalIndex.get();

        Optional<String> authHeader = ExchangeUtil.extractHeaderValue(exchange, "Authorization");
        if (!authHeader.isPresent()) {
            ResponseUtil.unauthorized(exchange);
            return;
        }

        Optional<String> apiKeyOptional = extractApiKey(authHeader.get());
        if (!apiKeyOptional.isPresent()) {
            ResponseUtil.unauthorized(exchange);
            return;
        }
        String apiKey = apiKeyOptional.get();

        if (!auth(indexResolver.checkIndex(apiKey, index), exchange)) {
            return;
        }

        int contentLength = ExchangeUtil.extractContentLength(exchange);
        if (contentLength < 0 || CommonConstants.MAX_MESSAGE_SIZE < contentLength) {
            ResponseUtil.badRequest(exchange);
            return;
        }

        Event[] events = ElasticAdapterUtil.createEventStream(exchange.getInputStream())
                .map(event -> patchEvent(event, eventBuilder -> eventBuilder.setTag("index", Variant.ofString(index))))
                .toArray(Event[]::new);

        byte[] body = EventWriterUtil.toBytes(contentLength, events);

        handler.handle(body);
    }

    private Event patchEvent(Event event, Consumer<EventBuilder> consumer) {
        EventBuilder eventBuilder = new EventBuilder();
        eventBuilder.setEventId(event.getId());
        eventBuilder.setVersion(event.getVersion());

        for (Map.Entry<String, Variant> entry : event) {
            eventBuilder.setTag(entry.getKey(), entry.getValue());
        }

        consumer.accept(eventBuilder);

        return eventBuilder.build();
    }

    private Optional<String> extractApiKey(String authHeader) {
        Matcher matcher = authHeaderPattern.matcher(authHeader);

        if (!matcher.matches()) {
            return Optional.empty();
        }

        try {
            return Optional.ofNullable(matcher.group(1));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private boolean auth(IndexResolver.Status status, HttpServerExchange exchange) {
        switch (status) {
            case UNKNOWN:
                ResponseUtil.unauthorized(exchange);
                return false;

            case FORBIDDEN:
                ResponseUtil.forbidden(exchange);
                return false;

            case OK:
            default:
                return true;
        }
    }

    /**
     * Functional interface processing Hercules protocol events gotten from request body
     */
    @FunctionalInterface
    public interface ElasticAdapterFunction {
        /**
         * Handle bytes of events
         *
         * @param content bytes of events in hercules protocol format
         */
        void handle(byte[] content);
    }
}
