package ru.kontur.vostok.hercules.stream.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.http.ContentEncodings;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.http.MimeTypes;
import ru.kontur.vostok.hercules.http.header.HttpHeaders;
import ru.kontur.vostok.hercules.meta.stream.Stream;
import ru.kontur.vostok.hercules.protocol.ByteStreamContent;
import ru.kontur.vostok.hercules.protocol.decoder.Decoder;
import ru.kontur.vostok.hercules.protocol.decoder.StreamReadStateReader;
import ru.kontur.vostok.hercules.protocol.encoder.ByteStreamContentWriter;
import ru.kontur.vostok.hercules.protocol.encoder.Encoder;
import ru.kontur.vostok.hercules.util.ByteBufferPool;
import ru.kontur.vostok.hercules.util.collection.ArrayUtil;
import ru.kontur.vostok.hercules.util.compression.Compressor;
import ru.kontur.vostok.hercules.util.compression.Lz4Compressor;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.time.TimeSource;

import java.nio.ByteBuffer;
import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class StreamReadRequestProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamReadRequestProcessor.class);

    private static final StreamReadStateReader STATE_READER = new StreamReadStateReader();
    private static final ByteStreamContentWriter CONTENT_WRITER = new ByteStreamContentWriter();

    private final Compressor compressor = new Lz4Compressor();

    private final StreamReader streamReader;

    private final TimeSource time;

    private final StreamReadRequestMetrics metrics;

    public StreamReadRequestProcessor(Properties properties, StreamReader streamReader, MetricsCollector metricsCollector) {
        this(properties, streamReader, metricsCollector, TimeSource.SYSTEM);
    }

    StreamReadRequestProcessor(Properties properties, StreamReader streamReader, MetricsCollector metricsCollector, TimeSource time) {
        this.streamReader = streamReader;
        this.time = time;

        this.metrics = new StreamReadRequestMetrics(PropertiesUtil.ofScope(properties, "metrics"), metricsCollector);
    }

    public void processAsync(HttpServerRequest request, Stream stream, int shardIndex, int shardCount, int take, int timeoutMs) {
        new StreamReadRequest(request, stream, shardIndex, shardCount, take, timeoutMs).processAsync();
    }

    public class StreamReadRequest {
        private final HttpServerRequest request;

        private final Stream stream;
        private final int shardIndex;
        private final int shardCount;
        private final int take;
        private final int timeoutMs;

        private final long processingStartedAtMs = time.milliseconds();
        private volatile long processingEndedAtMs;
        private volatile long readingStartedAtMs = Long.MAX_VALUE;
        private volatile long readingEndedAtMs;
        private volatile long compressionTimeMs;
        private volatile long sendingStartedAtMs = Long.MAX_VALUE;
        private volatile long sendingEndedAtMs;

        private volatile int uncompressedSizeBytes;
        private volatile int compressedSizeBytes;

        public StreamReadRequest(HttpServerRequest request, Stream stream, int shardIndex, int shardCount, int take, int timeoutMs) {
            this.request = request;

            this.stream = stream;
            this.shardIndex = shardIndex;
            this.shardCount = shardCount;
            this.take = take;
            this.timeoutMs = timeoutMs;
        }

        public void processAsync() {
            request.readBodyAsync(
                    (r, bytes) -> request.dispatchAsync(
                            () -> {
                                try {
                                    readingStartedAtMs = time.milliseconds();
                                    ByteStreamContent streamContent = streamReader.read(
                                            stream,
                                            STATE_READER.read(new Decoder(bytes)),
                                            shardIndex,
                                            shardCount,
                                            take,
                                            timeoutMs);
                                    readingEndedAtMs = time.milliseconds();

                                    sendAsync(streamContent);
                                } catch (IllegalArgumentException e) {
                                    request.complete(HttpStatusCodes.BAD_REQUEST);
                                } catch (Exception e) {
                                    LOGGER.error("Error on processing request", e);
                                    request.complete(HttpStatusCodes.INTERNAL_SERVER_ERROR);
                                }
                            }));
        }

        /**
         * Asynchronously send response to the client.
         * <p>
         * Compress response body if possible.
         *
         * @param streamContent response body content
         */
        private void sendAsync(ByteStreamContent streamContent) {
            request.getResponse().setContentType(MimeTypes.APPLICATION_OCTET_STREAM);

            uncompressedSizeBytes = compressedSizeBytes = streamContent.sizeOf();

            ByteBuffer buffer = ByteBufferPool.acquire(uncompressedSizeBytes);
            Encoder encoder = new Encoder(buffer);
            CONTENT_WRITER.write(encoder, streamContent);
            buffer.flip();

            // FIXME: Should be replaced with generic solution to support multiple compression algorithms
            if (ArrayUtil.contains(request.getHeaders(HttpHeaders.ACCEPT_ENCODING), ContentEncodings.LZ4)) {
                ByteBuffer compressed = compressLz4(buffer);
                compressedSizeBytes = compressed.remaining();
                ByteBufferPool.release(buffer);
                buffer = compressed;

                request.getResponse().setHeader(HttpHeaders.CONTENT_ENCODING, ContentEncodings.LZ4);
                request.getResponse().setHeader(HttpHeaders.ORIGINAL_CONTENT_LENGTH, String.valueOf(uncompressedSizeBytes));
            }

            final ByteBuffer bufferToSend = buffer;

            sendingStartedAtMs = TimeSource.SYSTEM.milliseconds();
            request.getResponse().setContentLength(compressedSizeBytes);
            request.getResponse().send(
                    buffer,
                    req -> {
                        processingEndedAtMs = sendingEndedAtMs = TimeSource.SYSTEM.milliseconds();
                        request.complete();
                        ByteBufferPool.release(bufferToSend);
                        metrics.update(this);
                    },
                    (req, exception) -> {
                        processingEndedAtMs = sendingEndedAtMs = TimeSource.SYSTEM.milliseconds();
                        LOGGER.error("Error when send response", exception);
                        request.complete();
                        ByteBufferPool.release(bufferToSend);
                        metrics.update(this);
                    });
        }

        private ByteBuffer compressLz4(ByteBuffer buffer) {
            int requiredCapacity = compressor.maxCompressedLength(buffer.remaining());
            ByteBuffer compressed = ByteBufferPool.acquire(requiredCapacity);
            compressionTimeMs = TimeSource.SYSTEM.measureMs(() -> compressor.compress(buffer, compressed));
            return compressed;
        }

        /**
         * The reading timeout is a value of {@code timeoutMs} query parameter or a default value.
         *
         * @return the reading timeout in millis
         */
        public int timeoutMs() {
            return timeoutMs;
        }

        public long processingTimeMs() {
            return Math.max(processingEndedAtMs - processingStartedAtMs, 0L);
        }

        public long readingTimeMs() {
            return Math.max(readingEndedAtMs - readingStartedAtMs, 0L);
        }

        public long compressionTimeMs() {
            return compressionTimeMs;
        }

        public long sendingTimeMs() {
            return Math.max(sendingEndedAtMs - sendingStartedAtMs, 0L);
        }

        public int uncompressedSizeBytes() {
            return uncompressedSizeBytes;
        }

        public int compressedSizeBytes() {
            return compressedSizeBytes;
        }
    }
}
