package ru.kontur.vostok.hercules.elastic.adapter;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import ru.kontur.vostok.hercules.elastic.adapter.util.ElasticAdapterUtil;
import ru.kontur.vostok.hercules.gateway.client.util.EventWriterUtil;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.encoder.EventBuilder;
import ru.kontur.vostok.hercules.undertow.util.ExchangeUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ElasticAdapterHandler implements HttpHandler {
    private final Consumer<byte[]> sender;

    public ElasticAdapterHandler(Consumer<byte[]> sender) {
        this.sender = sender;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        Optional<String> optionalIndex = ExchangeUtil.extractQueryParam(exchange, "index");
        if (!optionalIndex.isPresent()) {
            exchange.setStatusCode(400);
            exchange.endExchange();
            return;
        }

        //auth manager is not set
        Optional<String> auth = ExchangeUtil.extractHeaderValue(exchange, "Authorization");
        if (!auth.isPresent()) {
            exchange.setStatusCode(401);
            exchange.endExchange();
            return;
        }

        int contentLength = ExchangeUtil.extractContentLength(exchange);
        if (contentLength < 0 || contentLength > (1 << 21)) {
            exchange.setStatusCode(400);
            exchange.endExchange();
            return;
        }

        String JSONString = new BufferedReader(new InputStreamReader(exchange.getInputStream()))
                .lines()
                .collect(Collectors.joining());

        if (JSONString == null || JSONString.isEmpty()) {
            exchange.setStatusCode(400);
            exchange.endExchange();
            return;
        }

        //Extracting events and adding tag "index" to it
        String index = optionalIndex.get();
        Event[] events = ElasticAdapterUtil.createEventStream(JSONString)
                .map(event -> rebuildEvent(event, eventBuilder -> eventBuilder.setTag("index", Variant.ofString(index))))
                .toArray(Event[]::new);

        byte[] body = EventWriterUtil.toBytes(contentLength, events);

        sender.accept(body);
    }

    private Event rebuildEvent(Event event, Consumer<EventBuilder> consumer) {
        EventBuilder eventBuilder = new EventBuilder();
        eventBuilder.setEventId(event.getId());
        eventBuilder.setVersion(event.getVersion());
        event.getTags().forEach(eventBuilder::setTag);

        consumer.accept(eventBuilder);

        return eventBuilder.build();
    }
}
