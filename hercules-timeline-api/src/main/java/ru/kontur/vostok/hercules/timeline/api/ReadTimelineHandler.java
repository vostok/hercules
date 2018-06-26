package ru.kontur.vostok.hercules.timeline.api;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import ru.kontur.vostok.hercules.meta.timeline.Timeline;
import ru.kontur.vostok.hercules.meta.timeline.TimelineRepository;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.TimelineContent;
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
import java.util.Optional;

public class ReadTimelineHandler implements HttpHandler {

    private static final TimelineReadStateReader TIMELINE_READ_STATE_READER = new TimelineReadStateReader();
    private static final TimelineReadStateWriter TIMELINE_READ_STATE_WRITER = new TimelineReadStateWriter();

    private static final ArrayWriter<Event> EVENT_ARRAY_WRITER = new ArrayWriter<>(new EventWriter());

    private final TimelineRepository timelineRepository;
    private final TimelineReader timelineReader;

    public ReadTimelineHandler(TimelineRepository timelineRepository, TimelineReader timelineReader) {
        this.timelineRepository = timelineRepository;
        this.timelineReader = timelineReader;
    }

    @Override
    public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {
        httpServerExchange.getRequestReceiver().receiveFullBytes((exchange, message) -> {
            exchange.dispatch(() -> {
                try {
                    Map<String, Deque<String>> queryParameters = exchange.getQueryParameters();
                    String timelineName = queryParameters.get("timeline").getFirst();
                    int k = Integer.valueOf(queryParameters.get("k").getFirst());
                    int n = Integer.valueOf(queryParameters.get("n").getFirst());
                    int take = Integer.valueOf(queryParameters.get("take").getFirst());
                    long from = Long.valueOf(queryParameters.get("from").getFirst());
                    long to = Long.valueOf(queryParameters.get("to").getFirst());

                    Optional<Timeline> timeline = timelineRepository.read(timelineName);
                    if (!timeline.isPresent()) {
                        exchange.setStatusCode(404);
                        return;
                    }

                    TimelineReadState readState = TIMELINE_READ_STATE_READER.read(new Decoder(message));

                    TimelineContent timelineContent = timelineReader.readTimeline(timeline.get(), readState, k, n, take, from, to);

                    Encoder encoder = new Encoder();
                    TIMELINE_READ_STATE_WRITER.write(encoder, timelineContent.getReadState());
                    EVENT_ARRAY_WRITER.write(encoder, timelineContent.getEvents());

                    exchange.getResponseSender().send(ByteBuffer.wrap(encoder.getBytes()));
                } catch (Exception e) {
                    e.printStackTrace();
                    exchange.setStatusCode(500);
                } finally {
                    exchange.endExchange();
                }
            });
        });
    }
}
