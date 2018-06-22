package ru.kontur.vostok.hercules.timeline.api;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import ru.kontur.vostok.hercules.protocol.ByteStreamContent;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.TimelineReadState;
import ru.kontur.vostok.hercules.protocol.decoder.Decoder;
import ru.kontur.vostok.hercules.protocol.decoder.TimelineReadStateReader;
import ru.kontur.vostok.hercules.protocol.encoder.ArrayWriter;
import ru.kontur.vostok.hercules.protocol.encoder.Encoder;
import ru.kontur.vostok.hercules.protocol.encoder.EventWriter;
import ru.kontur.vostok.hercules.protocol.encoder.TimelineReadStateWriter;

import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.Map;

public class ReadTimelineHandler implements HttpHandler {

    private static final TimelineReadStateReader TIMELINE_READ_STATE_READER = new TimelineReadStateReader();
    private static final TimelineReadStateWriter TIMELINE_READ_STATE_WRITER = new TimelineReadStateWriter();

    private static final ArrayWriter<Event> EVENT_ARRAY_WRITER = new ArrayWriter<>(new EventWriter());

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


                    TimelineReadState readState = TIMELINE_READ_STATE_READER.read(new Decoder(message));

                    Encoder encoder = new Encoder();
                    TIMELINE_READ_STATE_WRITER.write(encoder, readState);
                    EVENT_ARRAY_WRITER.write(encoder, new Event[]{});

                    exchange.getResponseSender().send(ByteBuffer.wrap(encoder.getBytes()));
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
