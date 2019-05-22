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
import ru.kontur.vostok.hercules.util.functional.Result;
import ru.kontur.vostok.hercules.util.parsing.Parsers;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Optional;

public class ReadTimelineHandler implements HttpHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReadTimelineHandler.class);

    private static final TimelineStateReader STATE_READER = new TimelineStateReader();
    private static final TimelineByteContentWriter CONTENT_WRITER = new TimelineByteContentWriter();

    private static final String REASON_MISSING_PARAM = "Missing required parameter ";

    private static final String PARAM_TIMELINE = "timeline";
    private static final String PARAM_SHARD_INDEX = "shardIndex";
    private static final String PARAM_SHARD_COUNT = "shardCount";
    private static final String PARAM_TAKE = "take";
    private static final String PARAM_FROM = "from";
    private static final String PARAM_TO = "to";

    private final TimelineRepository timelineRepository;
    private final TimelineReader timelineReader;
    private final AuthManager authManager;
    private final int timetrapCountLimit;

    public ReadTimelineHandler(TimelineRepository timelineRepository, TimelineReader timelineReader, AuthManager authManager, int timetrapCountLimit) {
        this.timelineRepository = timelineRepository;
        this.timelineReader = timelineReader;
        this.authManager = authManager;
        this.timetrapCountLimit = timetrapCountLimit;
    }

    public static boolean isTimetrapCountLimitExceeded(long from, long to, long timetrapSize, int timetrapCountLimit) {
        return (to - from) >= TimeUtil.millisToTicks(timetrapCountLimit * timetrapSize);
    }

    @Override
    public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {

        Optional<Integer> optionalContentLength = ExchangeUtil.extractContentLength(httpServerExchange);
        if (!optionalContentLength.isPresent()) {
            ResponseUtil.lengthRequired(httpServerExchange);
            return;
        }

        Optional<String> optionalApiKey = ExchangeUtil.extractHeaderValue(httpServerExchange, "apiKey");
        if (!optionalApiKey.isPresent()) {
            ResponseUtil.unauthorized(httpServerExchange);
            return;
        }
        String apiKey = optionalApiKey.get();

        Optional<String> optionalTimelineName = ExchangeUtil.extractQueryParam(httpServerExchange, PARAM_TIMELINE);
        if (!optionalTimelineName.isPresent()) {
            ResponseUtil.badRequest(httpServerExchange, REASON_MISSING_PARAM + PARAM_TIMELINE);
            return;
        }

        AuthResult authResult = authManager.authRead(apiKey, optionalTimelineName.get());

        if (!authResult.isSuccess()) {
            if (authResult.isUnknown()) {
                ResponseUtil.unauthorized(httpServerExchange);
                return;
            }
            ResponseUtil.forbidden(httpServerExchange);
            return;
        }

        Optional<String> optionalShardIndex = ExchangeUtil.extractQueryParam(httpServerExchange, PARAM_SHARD_INDEX);
        if (!optionalShardIndex.isPresent()) {
            ResponseUtil.badRequest(httpServerExchange, REASON_MISSING_PARAM + PARAM_SHARD_INDEX);
            return;
        }

        Result<Integer, String> shardIndex = Parsers.parseInteger(optionalShardIndex.get());
        if (!shardIndex.isOk()) {
            ResponseUtil.badRequest(httpServerExchange, shardIndex.getError() + " in parameter " + PARAM_SHARD_INDEX);
            return;
        }

        Optional<String> optionalShardCount = ExchangeUtil.extractQueryParam(httpServerExchange, PARAM_SHARD_COUNT);
        if (!optionalShardCount.isPresent()) {
            ResponseUtil.badRequest(httpServerExchange, REASON_MISSING_PARAM + PARAM_SHARD_COUNT);
            return;
        }

        Result<Integer, String> shardCount = Parsers.parseInteger(optionalShardCount.get());
        if (!shardCount.isOk()) {
            ResponseUtil.badRequest(httpServerExchange, shardCount.getError() + " in parameter " + PARAM_SHARD_COUNT);
            return;
        }

        Optional<String> optionalTake = ExchangeUtil.extractQueryParam(httpServerExchange, PARAM_TAKE);
        if (!optionalTake.isPresent()) {
            ResponseUtil.badRequest(httpServerExchange, REASON_MISSING_PARAM + PARAM_TAKE);
            return;
        }

        Result<Integer, String> take = Parsers.parseInteger(optionalTake.get());
        if (!take.isOk()) {
            ResponseUtil.badRequest(httpServerExchange, take.getError() + " in parameter " + PARAM_TAKE);
            return;
        }

        Optional<String> optionalFrom = ExchangeUtil.extractQueryParam(httpServerExchange, PARAM_FROM);
        if (!optionalFrom.isPresent()) {
            ResponseUtil.badRequest(httpServerExchange, REASON_MISSING_PARAM + PARAM_FROM);
            return;
        }

        Result<Long, String> from = Parsers.parseLong(optionalFrom.get());
        if (!from.isOk()) {
            ResponseUtil.badRequest(httpServerExchange, take.getError() + " in parameter " + PARAM_FROM);
            return;
        }

        Optional<String> optionalTo = ExchangeUtil.extractQueryParam(httpServerExchange, PARAM_TO);
        if (!optionalTo.isPresent()) {
            ResponseUtil.badRequest(httpServerExchange, REASON_MISSING_PARAM + PARAM_TO);
            return;
        }

        Result<Long, String> to = Parsers.parseLong(optionalTo.get());
        if (!to.isOk()) {
            ResponseUtil.badRequest(httpServerExchange, take.getError() + " in parameter " + PARAM_TO);
            return;
        }

        Optional<Timeline> optionalTimeline = timelineRepository.read(optionalTimelineName.get());
        if (!optionalTimeline.isPresent()) {
            ResponseUtil.notFound(httpServerExchange);
            return;
        }

        Timeline timeline = optionalTimeline.get();
        if (isTimetrapCountLimitExceeded(from.get(), to.get(), timeline.getTimetrapSize(), timetrapCountLimit)) {
            ResponseUtil.badRequest(
                    httpServerExchange,
                    "Time interval should not exceeded " + TimeUtil.millisToTicks(timetrapCountLimit * timeline.getTimetrapSize()) + " ticks, but requested " + (to.get() - from.get()) + " ticks");
            return;
        }

        httpServerExchange.getRequestReceiver().receiveFullBytes((exchange, message) -> {
            exchange.dispatch(() -> {
                try {
                    TimelineState readState = STATE_READER.read(new Decoder(message));

                    TimelineByteContent byteContent = timelineReader.readTimeline(optionalTimeline.get(),
                            readState,
                            shardIndex.get(),
                            shardCount.get(),
                            take.get(),
                            from.get(),
                            to.get());

                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    Encoder encoder = new Encoder(stream);
                    CONTENT_WRITER.write(encoder, byteContent);

                    exchange.getResponseSender().send(ByteBuffer.wrap(stream.toByteArray()));
                } catch (Exception e) {
                    LOGGER.error("Error on processing request", e);
                    ResponseUtil.internalServerError(exchange);
                } finally {
                    exchange.endExchange();
                }
            });
        });
    }
}
