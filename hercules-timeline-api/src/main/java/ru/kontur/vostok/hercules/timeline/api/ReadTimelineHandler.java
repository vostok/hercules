package ru.kontur.vostok.hercules.timeline.api;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import ru.kontur.vostok.hercules.protocol.ByteStreamContent;
import ru.kontur.vostok.hercules.protocol.decoder.Decoder;
import ru.kontur.vostok.hercules.protocol.encoder.Encoder;

import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.Map;

public class ReadTimelineHandler implements HttpHandler {

    private final TimelineReader timelineReader = new TimelineReader();

    @Override
    public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {
        httpServerExchange.getRequestReceiver().receiveFullBytes((exchange, message) -> {
            exchange.dispatch(() -> {
                try {
                    Map<String, Deque<String>> queryParameters = exchange.getQueryParameters();
                    String streamName = queryParameters.get("stream").getFirst();
                    int k = Integer.valueOf(queryParameters.get("k").getFirst());
                    int n = Integer.valueOf(queryParameters.get("n").getFirst());
                    int take = Integer.valueOf(queryParameters.get("take").getFirst());
                    long from = Long.valueOf(queryParameters.get("from").getFirst());
                    long to = Long.valueOf(queryParameters.get("to").getFirst());



                    exchange.getResponseSender().send(ByteBuffer.wrap("".getBytes()));
                } catch (Exception e) {
                    e.printStackTrace();
                    exchange.setStatusCode(500);
                }
                finally {
                    exchange.endExchange();
                }
            });
        });
    }
}
