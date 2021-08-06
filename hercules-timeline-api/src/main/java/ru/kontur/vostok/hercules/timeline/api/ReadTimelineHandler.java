package ru.kontur.vostok.hercules.timeline.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.auth.AuthProvider;
import ru.kontur.vostok.hercules.auth.AuthResult;
import ru.kontur.vostok.hercules.curator.exception.CuratorException;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.http.MimeTypes;
import ru.kontur.vostok.hercules.http.handler.HttpHandler;
import ru.kontur.vostok.hercules.http.query.QueryUtil;
import ru.kontur.vostok.hercules.meta.serialization.DeserializationException;
import ru.kontur.vostok.hercules.meta.timeline.Timeline;
import ru.kontur.vostok.hercules.meta.timeline.TimelineRepository;
import ru.kontur.vostok.hercules.protocol.TimelineByteContent;
import ru.kontur.vostok.hercules.protocol.TimelineState;
import ru.kontur.vostok.hercules.protocol.decoder.Decoder;
import ru.kontur.vostok.hercules.protocol.decoder.TimelineStateReader;
import ru.kontur.vostok.hercules.protocol.encoder.Encoder;
import ru.kontur.vostok.hercules.protocol.encoder.TimelineByteContentWriter;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.nio.ByteBuffer;
import java.util.Optional;

/**
 * @author Gregory Koshelev
 */
public class ReadTimelineHandler implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReadTimelineHandler.class);

    private static final TimelineStateReader STATE_READER = new TimelineStateReader();
    private static final TimelineByteContentWriter CONTENT_WRITER = new TimelineByteContentWriter();

    private final TimelineRepository timelineRepository;
    private final TimelineReader timelineReader;
    private final int timetrapCountLimit;
    private final AuthProvider authProvider;

    public ReadTimelineHandler(AuthProvider authProvider, TimelineRepository timelineRepository, TimelineReader timelineReader) {
        this.authProvider = authProvider;
        this.timelineRepository = timelineRepository;
        this.timelineReader = timelineReader;
        this.timetrapCountLimit = timelineReader.getTimetrapCountLimit();
    }

    public static boolean isTimetrapCountLimitExceeded(long from, long to, long timetrapSize, int timetrapCountLimit) {
        return (to - from) >= TimeUtil.millisToTicks(timetrapCountLimit * timetrapSize);
    }

    @Override
    public void handle(HttpServerRequest request) {
        Optional<Integer> optionalContentLength = request.getContentLength();
        if (!optionalContentLength.isPresent()) {
            request.complete(HttpStatusCodes.LENGTH_REQUIRED);
            return;
        }

        Parameter<String>.ParameterValue timelineName = QueryUtil.get(QueryParameters.TIMELINE, request);
        if (QueryUtil.tryCompleteRequestIfError(request, timelineName)) {
            return;
        }

        AuthResult authResult = authProvider.authRead(request, timelineName.get());
        if (!authResult.isSuccess()) {
            if (authResult.isUnknown()) {
                request.complete(HttpStatusCodes.UNAUTHORIZED);
                return;
            }
            request.complete(HttpStatusCodes.FORBIDDEN);
            return;
        }

        Parameter<Integer>.ParameterValue shardIndex = QueryUtil.get(QueryParameters.SHARD_INDEX, request);
        if (QueryUtil.tryCompleteRequestIfError(request, shardIndex)) {
            return;
        }

        Parameter<Integer>.ParameterValue shardCount = QueryUtil.get(QueryParameters.SHARD_COUNT, request);
        if (QueryUtil.tryCompleteRequestIfError(request, shardCount)) {
            return;
        }

        if (shardCount.get() <= shardIndex.get()) {
            request.complete(
                    HttpStatusCodes.BAD_REQUEST,
                    MimeTypes.TEXT_PLAIN,
                    "Invalid parameters: " + QueryParameters.SHARD_COUNT.name() + " must be > " + QueryParameters.SHARD_INDEX.name());
            return;
        }

        Parameter<Integer>.ParameterValue take = QueryUtil.get(QueryParameters.TAKE, request);
        if (QueryUtil.tryCompleteRequestIfError(request, take)) {
            return;
        }

        Parameter<Long>.ParameterValue from = QueryUtil.get(QueryParameters.FROM, request);
        if (QueryUtil.tryCompleteRequestIfError(request, from)) {
            return;
        }

        Parameter<Long>.ParameterValue to = QueryUtil.get(QueryParameters.TO, request);
        if (QueryUtil.tryCompleteRequestIfError(request, to)) {
            return;
        }

        Timeline timeline;
        try {
            Optional<Timeline> optionalTimeline = timelineRepository.read(timelineName.get());
            if (!optionalTimeline.isPresent()) {
                request.complete(HttpStatusCodes.NOT_FOUND);
                return;
            }
            timeline = optionalTimeline.get();
        } catch (CuratorException ex) {
            LOGGER.error("Curator exception when read Stream", ex);
            request.complete(HttpStatusCodes.INTERNAL_SERVER_ERROR);
            return;
        } catch (DeserializationException ex) {
            LOGGER.error("Deserialization exception of Stream", ex);
            request.complete(HttpStatusCodes.INTERNAL_SERVER_ERROR);
            return;
        }

        if (isTimetrapCountLimitExceeded(from.get(), to.get(), timeline.getTimetrapSize(), timetrapCountLimit)) {
            request.complete(
                    HttpStatusCodes.BAD_REQUEST,
                    MimeTypes.TEXT_PLAIN,
                    "Time interval should not exceeded " + TimeUtil.millisToTicks(timetrapCountLimit * timeline.getTimetrapSize()) + " ticks, but requested " + (to.get() - from.get()) + " ticks");
            return;
        }

        request.readBodyAsync((r, bytes) -> request.dispatchAsync(
                () -> {
                    try {
                        TimelineState readState = STATE_READER.read(new Decoder(bytes));

                        TimelineByteContent byteContent = timelineReader.readTimeline(
                                timeline,
                                readState,
                                shardIndex.get(),
                                shardCount.get(),
                                take.get(),
                                from.get(),
                                to.get());

                        ByteBuffer buffer = ByteBuffer.allocate(byteContent.sizeOf());
                        Encoder encoder = new Encoder(buffer);
                        CONTENT_WRITER.write(encoder, byteContent);
                        buffer.flip();
                        request.getResponse().send(buffer);
                    } catch (Exception e) {
                        LOGGER.error("Error on processing request", e);
                        request.complete(HttpStatusCodes.INTERNAL_SERVER_ERROR);
                    }
                }));
    }
}
