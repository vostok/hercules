package ru.kontur.vostok.hercules.stream.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.auth.AuthProvider;
import ru.kontur.vostok.hercules.auth.AuthResult;
import ru.kontur.vostok.hercules.curator.exception.CuratorException;
import ru.kontur.vostok.hercules.http.ContentEncodings;
import ru.kontur.vostok.hercules.http.header.HttpHeaders;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.http.MimeTypes;
import ru.kontur.vostok.hercules.http.handler.HttpHandler;
import ru.kontur.vostok.hercules.http.query.QueryUtil;
import ru.kontur.vostok.hercules.meta.serialization.DeserializationException;
import ru.kontur.vostok.hercules.meta.stream.Stream;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.protocol.ByteStreamContent;
import ru.kontur.vostok.hercules.protocol.decoder.Decoder;
import ru.kontur.vostok.hercules.protocol.decoder.StreamReadStateReader;
import ru.kontur.vostok.hercules.protocol.encoder.ByteStreamContentWriter;
import ru.kontur.vostok.hercules.protocol.encoder.Encoder;
import ru.kontur.vostok.hercules.util.ByteBufferPool;
import ru.kontur.vostok.hercules.util.collection.ArrayUtil;
import ru.kontur.vostok.hercules.util.compression.Compressor;
import ru.kontur.vostok.hercules.util.compression.Lz4Compressor;
import ru.kontur.vostok.hercules.util.parameter.ParameterValue;

import java.nio.ByteBuffer;
import java.util.Optional;

/**
 * @author Gregory Koshelev
 */
public class StreamReadHandler implements HttpHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(StreamReadHandler.class);

    private static final StreamReadStateReader STATE_READER = new StreamReadStateReader();
    private static final ByteStreamContentWriter CONTENT_WRITER = new ByteStreamContentWriter();

    private final Compressor compressor = new Lz4Compressor();

    private final AuthProvider authProvider;
    private final StreamReader streamReader;
    private final StreamRepository streamRepository;

    public StreamReadHandler(AuthProvider authProvider, StreamRepository streamRepository, StreamReader streamReader) {
        this.authProvider = authProvider;
        this.streamRepository = streamRepository;
        this.streamReader = streamReader;
    }

    @Override
    public void handle(HttpServerRequest request) {
        Optional<Integer> optionalContentLength = request.getContentLength();
        if (!optionalContentLength.isPresent()) {
            request.complete(HttpStatusCodes.LENGTH_REQUIRED);
            return;
        }

        ParameterValue<String> streamName = QueryUtil.get(QueryParameters.STREAM, request);
        if (!streamName.isOk()) {
            request.complete(
                    HttpStatusCodes.BAD_REQUEST,
                    MimeTypes.TEXT_PLAIN,
                    "Parameter " + QueryParameters.STREAM.name() + " error: " + streamName.result().error());
            return;
        }

        AuthResult authResult = authProvider.authRead(request, streamName.get());
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

        ParameterValue<Integer> timeoutMs = QueryUtil.get(QueryParameters.TIMEOUT_MS, request);
        if (!timeoutMs.isOk()) {
            request.complete(
                    HttpStatusCodes.BAD_REQUEST,
                    MimeTypes.TEXT_PLAIN,
                    "Parameter " + QueryParameters.TIMEOUT_MS.name() + " error: " + timeoutMs.result().error());
            return;
        }

        request.readBodyAsync(
                (r, bytes) -> request.dispatchAsync(
                        () -> {
                            try {
                                ByteStreamContent streamContent = streamReader.read(
                                        stream,
                                        STATE_READER.read(new Decoder(bytes)),
                                        shardIndex.get(),
                                        shardCount.get(),
                                        take.get(),
                                        timeoutMs.get());

                                request.getResponse().setContentType(MimeTypes.APPLICATION_OCTET_STREAM);

                                ByteBuffer buffer = ByteBufferPool.acquire(streamContent.sizeOf());
                                Encoder encoder = new Encoder(buffer);
                                CONTENT_WRITER.write(encoder, streamContent);
                                buffer.flip();

                                // FIXME: Should be replaced with generic solution to support multiple compression algorithms
                                if (ArrayUtil.contains(request.getHeaders(HttpHeaders.ACCEPT_ENCODING), ContentEncodings.LZ4)) {
                                    int requiredCapacity = compressor.maxCompressedLength(buffer.remaining());
                                    ByteBuffer compressed = ByteBufferPool.acquire(requiredCapacity);
                                    compressor.compress(buffer, compressed);
                                    ByteBufferPool.release(buffer);
                                    buffer = compressed;

                                    request.getResponse().setHeader(HttpHeaders.CONTENT_ENCODING, ContentEncodings.LZ4);
                                    request.getResponse().setHeader(HttpHeaders.ORIGINAL_CONTENT_LENGTH, String.valueOf(streamContent.sizeOf()));
                                }

                                send(request, buffer);
                            } catch (IllegalArgumentException e) {
                                request.complete(HttpStatusCodes.BAD_REQUEST);
                            } catch (Exception e) {
                                LOGGER.error("Error on processing request", e);
                                request.complete(HttpStatusCodes.INTERNAL_SERVER_ERROR);
                            }
                        }));
    }

    /**
     * Send data to the client.
     * <p>
     * Note, {@link ByteBuffer buffer} will be released to {@link ByteBufferPool} after request completion.
     *
     * @param request the request
     * @param buffer  the data buffer
     */
    private void send(HttpServerRequest request, ByteBuffer buffer) {
        request.getResponse().setContentLength(buffer.remaining());
        request.getResponse().send(
                buffer,
                req -> {
                    request.complete();
                    ByteBufferPool.release(buffer);
                },
                (req, exception) -> {
                    LOGGER.error("Error when send response", exception);
                    request.complete();
                    ByteBufferPool.release(buffer);
                });
    }
}
