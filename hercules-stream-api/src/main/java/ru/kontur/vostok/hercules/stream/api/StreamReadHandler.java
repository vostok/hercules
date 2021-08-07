package ru.kontur.vostok.hercules.stream.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.auth.AuthProvider;
import ru.kontur.vostok.hercules.auth.AuthResult;
import ru.kontur.vostok.hercules.auth.AuthUtil;
import ru.kontur.vostok.hercules.curator.exception.CuratorException;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.http.MimeTypes;
import ru.kontur.vostok.hercules.http.handler.HttpHandler;
import ru.kontur.vostok.hercules.http.query.QueryUtil;
import ru.kontur.vostok.hercules.meta.serialization.DeserializationException;
import ru.kontur.vostok.hercules.meta.stream.Stream;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.util.parameter.Parameter;

import java.util.Optional;

/**
 * @author Gregory Koshelev
 */
public class StreamReadHandler implements HttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamReadHandler.class);

    private final AuthProvider authProvider;
    private final StreamReadRequestProcessor processor;
    private final StreamRepository streamRepository;

    public StreamReadHandler(
            AuthProvider authProvider,
            StreamRepository streamRepository,
            StreamReadRequestProcessor processor) {
        this.authProvider = authProvider;
        this.streamRepository = streamRepository;
        this.processor = processor;
    }

    @Override
    public void handle(HttpServerRequest request) {
        Optional<Integer> optionalContentLength = request.getContentLength();
        if (!optionalContentLength.isPresent()) {
            request.complete(HttpStatusCodes.LENGTH_REQUIRED);
            return;
        }

        Parameter<String>.ParameterValue streamName = QueryUtil.get(QueryParameters.STREAM, request);
        if (QueryUtil.tryCompleteRequestIfError(request, streamName)) {
            return;
        }

        AuthResult authResult = authProvider.authRead(request, streamName.get());
        if (AuthUtil.tryCompleteRequestIfUnsuccessfulAuth(request, authResult)) {
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

        Stream stream;
        try {
            Optional<Stream> optionalStream = streamRepository.read(streamName.get());
            if (!optionalStream.isPresent()) {
                request.complete(HttpStatusCodes.NOT_FOUND);
                return;
            }
            stream = optionalStream.get();
        } catch (CuratorException ex) {
            LOGGER.error("Curator exception when read Stream", ex);
            request.complete(HttpStatusCodes.INTERNAL_SERVER_ERROR);
            return;
        } catch (DeserializationException ex) {
            LOGGER.error("Deserialization exception of Stream", ex);
            request.complete(HttpStatusCodes.INTERNAL_SERVER_ERROR);
            return;
        }

        Parameter<Integer>.ParameterValue timeoutMs = QueryUtil.get(QueryParameters.TIMEOUT_MS, request);
        if (QueryUtil.tryCompleteRequestIfError(request, timeoutMs)) {
            return;
        }

        processor.processAsync(request, stream, shardIndex.get(), shardCount.get(), take.get(), timeoutMs.get());
    }
}
