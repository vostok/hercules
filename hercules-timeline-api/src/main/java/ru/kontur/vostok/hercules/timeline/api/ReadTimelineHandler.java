package ru.kontur.vostok.hercules.timeline.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.auth.AuthManager;
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
import ru.kontur.vostok.hercules.util.parameter.ParameterValue;
import ru.kontur.vostok.hercules.util.text.StringUtil;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Optional;

/**
 * @author Gregory Koshelev
 */
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

    public ReadTimelineHandler(TimelineRepository timelineRepository, TimelineReader timelineReader, AuthManager authManager) {
        this.timelineRepository = timelineRepository;
        this.timelineReader = timelineReader;
        this.authManager = authManager;
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

        String apiKey = request.getHeader("apiKey");
        if (StringUtil.isNullOrEmpty(apiKey)) {
            request.complete(HttpStatusCodes.UNAUTHORIZED);
            return;
        }

        ParameterValue<String> timelineName = QueryUtil.get(QueryParameters.TIMELINE, request);
        if (!timelineName.isOk()) {
            request.complete(
                    HttpStatusCodes.BAD_REQUEST,
                    MimeTypes.TEXT_PLAIN,
                    "Parameter " + QueryParameters.TIMELINE.name() + " error: " + timelineName.result().error());
            return;
        }

        AuthResult authResult = authManager.authRead(apiKey, timelineName.get());

        if (!authResult.isSuccess()) {
            if (authResult.isUnknown()) {
                request.complete(HttpStatusCodes.UNAUTHORIZED);
                return;
            }
            request.complete(HttpStatusCodes.FORBIDDEN);
            return;
        }

        ParameterValue<Integer> shardIndex = QueryUtil.get(QueryParameters.SHARD_INDEX, request);
        if (!shardIndex.isOk()) {
            request.complete(
                    HttpStatusCodes.BAD_REQUEST,
                    MimeTypes.TEXT_PLAIN,
                    "Parameter " + QueryParameters.SHARD_INDEX.name() + " error: " + shardIndex.result().error());
            return;
        }

        ParameterValue<Integer> shardCount = QueryUtil.get(QueryParameters.SHARD_COUNT, request);
        if (!shardCount.isOk()) {
            request.complete(
                    HttpStatusCodes.BAD_REQUEST,
                    MimeTypes.TEXT_PLAIN,
                    "Parameter " + QueryParameters.SHARD_COUNT.name() + " error: " + shardCount.result().error());
            return;
        }

        if (shardCount.get() <= shardIndex.get()) {
            request.complete(
                    HttpStatusCodes.BAD_REQUEST,
                    MimeTypes.TEXT_PLAIN,
                    "Invalid parameters: " + QueryParameters.SHARD_COUNT.name() + " must be > " + QueryParameters.SHARD_INDEX.name());
            return;
        }

        ParameterValue<Integer> take = QueryUtil.get(QueryParameters.TAKE, request);
        if (!take.isOk()) {
            request.complete(
                    HttpStatusCodes.BAD_REQUEST,
                    MimeTypes.TEXT_PLAIN,
                    "Parameter " + QueryParameters.TAKE.name() + " error: " + take.result().error());
            return;
        }

        ParameterValue<Long> from = QueryUtil.get(QueryParameters.FROM, request);
        if (!from.isOk()) {
            request.complete(
                    HttpStatusCodes.BAD_REQUEST,
                    MimeTypes.TEXT_PLAIN,
                    "Parameter " + QueryParameters.FROM.name() + " error: " + from.result().error());
            return;
        }

        ParameterValue<Long> to = QueryUtil.get(QueryParameters.TO, request);
        if (!to.isOk()) {
            request.complete(
                    HttpStatusCodes.BAD_REQUEST,
                    MimeTypes.TEXT_PLAIN,
                    "Parameter " + QueryParameters.TO.name() + " error: " + to.result().error());
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

                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        Encoder encoder = new Encoder(stream);
                        CONTENT_WRITER.write(encoder, byteContent);

                        request.getResponse().send(ByteBuffer.wrap(stream.toByteArray()));
                    } catch (Exception e) {
                        LOGGER.error("Error on processing request", e);
                        request.complete(HttpStatusCodes.INTERNAL_SERVER_ERROR);
                    }
                }));
    }


}
