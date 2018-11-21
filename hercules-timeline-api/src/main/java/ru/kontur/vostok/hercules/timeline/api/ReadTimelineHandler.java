package ru.kontur.vostok.hercules.timeline.api;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.meta.timeline.Timeline;
import ru.kontur.vostok.hercules.meta.timeline.TimelineRepository;
import ru.kontur.vostok.hercules.protocol.TimelineByteContent;
import ru.kontur.vostok.hercules.protocol.TimelineReadState;
import ru.kontur.vostok.hercules.protocol.decoder.Decoder;
import ru.kontur.vostok.hercules.protocol.decoder.TimelineReadStateReader;
import ru.kontur.vostok.hercules.protocol.encoder.Encoder;
import ru.kontur.vostok.hercules.protocol.encoder.TimelineByteContentWriter;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;

public class ReadTimelineHandler implements HttpHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReadTimelineHandler.class);

    private static final TimelineReadStateReader STATE_READER = new TimelineReadStateReader();
    private static final TimelineByteContentWriter CONTENT_WRITER = new TimelineByteContentWriter();

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

                    TimelineReadState readState = STATE_READER.read(new Decoder(message));

                    TimelineByteContent byteContent = timelineReader.readTimeline(timeline.get(), readState, k, n, take, from, to);

                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    Encoder encoder = new Encoder(stream);
                    CONTENT_WRITER.write(encoder, byteContent);

                    exchange.getResponseSender().send(ByteBuffer.wrap(stream.toByteArray()));
                } catch (Exception e) {
                    LOGGER.error("Error on processing request", e);
                    exchange.setStatusCode(500);
                } finally {
                    exchange.endExchange();
                }
            });
        });
    }
}
