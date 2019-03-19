package ru.kontur.vostok.hercules.timeline.api;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.auth.AuthResult;
import ru.kontur.vostok.hercules.meta.timeline.Timeline;
import ru.kontur.vostok.hercules.meta.timeline.TimelineRepository;
import ru.kontur.vostok.hercules.protocol.TimelineByteContent;
import ru.kontur.vostok.hercules.protocol.TimelineState;
import ru.kontur.vostok.hercules.protocol.decoder.Decoder;
import ru.kontur.vostok.hercules.protocol.decoder.TimelineStateReader;
import ru.kontur.vostok.hercules.protocol.encoder.Encoder;
import ru.kontur.vostok.hercules.protocol.encoder.TimelineByteContentWriter;
import ru.kontur.vostok.hercules.undertow.util.ExchangeUtil;
import ru.kontur.vostok.hercules.undertow.util.ResponseUtil;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;

public class ReadTimelineHandler implements HttpHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReadTimelineHandler.class);

    private static final TimelineStateReader STATE_READER = new TimelineStateReader();
    private static final TimelineByteContentWriter CONTENT_WRITER = new TimelineByteContentWriter();

    private final AuthManager authManager;
    private final TimelineRepository timelineRepository;
    private final TimelineReader timelineReader;

    public ReadTimelineHandler(TimelineRepository timelineRepository, TimelineReader timelineReader, AuthManager authManager) {
        this.timelineRepository = timelineRepository;
        this.timelineReader = timelineReader;
        this.authManager = authManager;
    }

    @Override
    public void handleRequest(HttpServerExchange httpServerExchange) throws  Exception {

        Optional<Integer> optionalContentLength = ExchangeUtil.extractContentLength(httpServerExchange);
        if (!optionalContentLength.isPresent()) {
            ResponseUtil.lengthRequired(httpServerExchange);
            return;
        }
        if (optionalContentLength.get() < 0) {
            ResponseUtil.badRequest(httpServerExchange);
            return;
        }

        Optional<String> optionalApiKey = ExchangeUtil.extractHeaderValue(httpServerExchange, "apiKey");
        if (!optionalApiKey.isPresent()) {
            ResponseUtil.unauthorized(httpServerExchange);
            return;
        }
        Map<String, Deque<String>> queryParameters = httpServerExchange.getQueryParameters();

        String apiKey = optionalApiKey.get();
        String timelineName = queryParameters.get("timeline").getFirst();

        AuthResult authResult = authManager.authRead(apiKey, timelineName);

        if (!authResult.isSuccess()) {
            if (authResult.isUnknown()) {
                ResponseUtil.unauthorized(httpServerExchange);
                return;
            }
            ResponseUtil.forbidden(httpServerExchange);
            return;
        }

        int shardIndex = Integer.valueOf(queryParameters.get("shardIndex").getFirst());
        int shardCount = Integer.valueOf(queryParameters.get("shardCount").getFirst());
        int take = Integer.valueOf(queryParameters.get("take").getFirst());
        long from = Long.valueOf(queryParameters.get("from").getFirst());
        long to = Long.valueOf(queryParameters.get("to").getFirst());

        Optional<Timeline> timeline = timelineRepository.read(timelineName);
        if (!timeline.isPresent()) {
            ResponseUtil.notFound(httpServerExchange);
            return;
        }

        httpServerExchange.getRequestReceiver().receiveFullBytes((exchange, message) -> {
            exchange.dispatch(() -> {
                try {
                    TimelineState readState = STATE_READER.read(new Decoder(message));

                    TimelineByteContent byteContent = timelineReader.readTimeline(timeline.get(), readState, shardIndex, shardCount, take, from, to);

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
