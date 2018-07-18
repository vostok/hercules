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

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Http Handler for handling requests which are sent to elastic adapter.
 * Process logs. Convert json to Hercules protocol event format.
 *
 * @author Daniil Zhenikhov
 */
public class ElasticAdapterHandler implements HttpHandler {

    private final ElasticAdapterFunction handler;

    public ElasticAdapterHandler(ElasticAdapterFunction handler) {
        this.handler = handler;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        Optional<String> optionalIndex = ExchangeUtil.extractQueryParam(exchange, "index");
        if (!optionalIndex.isPresent()) {
            exchange.setStatusCode(400);
            exchange.endExchange();
            return;
        }

        //apiKey manager is not set
        Optional<String> apiKey = ExchangeUtil.extractHeaderValue(exchange, "apiKey");
        if (!apiKey.isPresent()) {
            exchange.setStatusCode(401);
            exchange.endExchange();
            return;
        }

        int contentLength = ExchangeUtil.extractContentLength(exchange);
        if (contentLength < 0 || CommonConstants.MAX_MESSAGE_SIZE < contentLength) {
            exchange.setStatusCode(400);
            exchange.endExchange();
            return;
        }

        //Extracting events and adding tag "index" to it
        String index = optionalIndex.get();
        Event[] events = ElasticAdapterUtil.createEventStream(exchange.getInputStream())
                .map(event -> patchEvent(event, eventBuilder -> eventBuilder.setTag("index", Variant.ofString(index))))
                .toArray(Event[]::new);

        byte[] body = EventWriterUtil.toBytes(contentLength, events);

        handler.handle(apiKey.get(), body);
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

    /**
     * Functional interface processing Hercules protocol events gotten from request body
     */
    @FunctionalInterface
    public interface ElasticAdapterFunction {
        /**
         * Handle bytes of events
         *
         * @param apiKey extracted from query
         * @param content bytes of events in hercules protocol format
         */
        void handle(String apiKey, byte[] content);
    }
}
