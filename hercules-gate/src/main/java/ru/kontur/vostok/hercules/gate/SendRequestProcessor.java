package ru.kontur.vostok.hercules.gate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import ru.kontur.vostok.hercules.gate.validation.EventValidator;
import ru.kontur.vostok.hercules.health.Histogram;
import ru.kontur.vostok.hercules.health.Meter;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.http.ContentEncodings;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.http.header.HeaderUtil;
import ru.kontur.vostok.hercules.http.header.HttpHeaders;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.decoder.Decoder;
import ru.kontur.vostok.hercules.protocol.decoder.EventReader;
import ru.kontur.vostok.hercules.protocol.decoder.ReaderIterator;
import ru.kontur.vostok.hercules.protocol.decoder.exceptions.InvalidDataException;
import ru.kontur.vostok.hercules.util.ByteBufferPool;
import ru.kontur.vostok.hercules.util.compression.Lz4Decompressor;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.text.StringUtil;
import ru.kontur.vostok.hercules.util.time.TimeSource;
import ru.kontur.vostok.hercules.util.validation.IntegerValidators;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Gregory Koshelev
 */
public class SendRequestProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(SendRequestProcessor.class);

    private static final Parameter<Integer> ORIGINAL_CONTENT_LENGTH =
            Parameter.integerParameter(HttpHeaders.ORIGINAL_CONTENT_LENGTH).
                    required().
                    withValidator(IntegerValidators.range(0, 100 * 1024 * 1024)).
                    build();

    private final EventSender eventSender;
    private final TimeSource time;

    private final EventValidator eventValidator;
    private final Lz4Decompressor lz4Decompressor = new Lz4Decompressor();

    private final SendRequestMetrics metrics;

    private final Meter sentEventsMeter;
    private final Meter lostEventsMeter;
    private final Histogram recvEventsHistogram;
    private final Histogram eventSizeHistogram;

    public SendRequestProcessor(EventSender eventSender, EventValidator eventValidator, MetricsCollector metricsCollector) {
        this(eventSender, eventValidator, metricsCollector, TimeSource.SYSTEM);
    }

    SendRequestProcessor(EventSender eventSender, EventValidator eventValidator, MetricsCollector metricsCollector, TimeSource time) {
        this.eventSender = eventSender;
        this.eventValidator = eventValidator;
        this.time = time;
        this.metrics = new SendRequestMetrics(metricsCollector);

        this.sentEventsMeter = metricsCollector.meter(this.getClass().getSimpleName() + ".sentEvents");
        this.lostEventsMeter = metricsCollector.meter(this.getClass().getSimpleName() + ".lostEvents");
        this.recvEventsHistogram = metricsCollector.histogram(this.getClass().getSimpleName() + ".recvEvents");
        this.eventSizeHistogram = metricsCollector.histogram(this.getClass().getSimpleName() + ".eventSize");
    }

    public void processAsync(HttpServerRequest request, SendRequestContext context, Callback callback) {
        new SendRequest(request, context).processAsync(callback);
    }

    private void sendAsync(SendRequest request, ByteBuffer buffer, Callback callback) {
        ReaderIterator<Event> reader;
        try {
            reader = new ReaderIterator<>(new Decoder(buffer), EventReader.readTags(request.context.getTags()));
        } catch (InvalidDataException ex) {
            request.tryComplete(HttpStatusCodes.BAD_REQUEST, callback);
            LOGGER.error("Request is malformed", ex);
            return;
        }
        if (reader.getTotal() == 0) {
            request.tryComplete(HttpStatusCodes.OK, callback);
            return;
        }

        AtomicInteger pendingEvents = new AtomicInteger(reader.getTotal());
        AtomicBoolean processed = new AtomicBoolean(false);
        recvEventsHistogram.update(reader.getTotal());
        while (reader.hasNext()) {
            Event event;
            try {
                event = reader.next();
                eventSizeHistogram.update(event.sizeOf());
                if (!eventValidator.validate(event)) {
                    if (processed.compareAndSet(false, true)) {
                        request.tryComplete(HttpStatusCodes.BAD_REQUEST, callback);
                    }
                    //TODO: Metrics are coming!
                    LOGGER.warn("Invalid event data");
                    return;
                }
            } catch (Exception ex) {
                if (processed.compareAndSet(false, true)) {
                    request.tryComplete(HttpStatusCodes.BAD_REQUEST, callback);
                }
                LOGGER.error("Exception on validation event", ex);
                //TODO: Metrics are coming!
                return;
            }
            if (!request.validator().validate(event)) {
                //TODO: should to log filtered events
                if (pendingEvents.decrementAndGet() == 0 && processed.compareAndSet(false, true)) {
                    if (!request.isAsync()) {
                        request.tryComplete(HttpStatusCodes.OK, callback);
                    } else {
                        callback.call();
                    }
                }
                continue;
            }
            eventSender.send(
                    event,
                    event.getUuid(),//TODO: Think hard about this!
                    request.context.getTopic(),
                    request.context.getPartitions(),
                    request.context.getShardingKey(),
                    () -> {
                        if (pendingEvents.decrementAndGet() == 0 && processed.compareAndSet(false, true)) {
                            if (!request.isAsync()) {
                                request.tryComplete(HttpStatusCodes.OK, callback);
                            } else {
                                callback.call();
                            }
                        }
                        sentEventsMeter.mark();
                    },
                    () -> {
                        if (processed.compareAndSet(false, true)) {
                            if (!request.isAsync()) {
                                request.tryComplete(HttpStatusCodes.INTERNAL_SERVER_ERROR, callback);
                            } else {
                                callback.call();
                            }
                        }
                        lostEventsMeter.mark();
                    }
            );
        }
        if (request.isAsync()) {
            request.tryComplete(HttpStatusCodes.OK, Callback.empty());
        }
    }

    /**
     * @author Gregory Koshelev
     */
    public class SendRequest {

        private final HttpServerRequest request;
        private final SendRequestContext context;

        private volatile long receivingStartedAtMs = Long.MAX_VALUE;
        private volatile long receivingEndedAtMs = Long.MAX_VALUE;
        private volatile long decompressionTimeMs;
        private volatile long processingTimestamp = Long.MAX_VALUE;
        private volatile long completionTimestamp = Long.MAX_VALUE;

        public SendRequest(HttpServerRequest request, SendRequestContext context) {
            this.request = request;
            this.context = context;
        }

        /**
         * Asynchronously process the request and call completion callback when processing has been completed.
         *
         * @param callback completion callback
         */
        public void processAsync(Callback callback) {
            receivingStartedAtMs = time.milliseconds();
            try {
                request.readBodyAsync(
                        (r, bytes) -> r.dispatchAsync(() -> {
                            receivingEndedAtMs = time.milliseconds();

                            ByteBuffer buffer = null;
                            try {
                                initMDC();

                                String contentEncoding = request.getHeader(HttpHeaders.CONTENT_ENCODING);
                                if (contentEncoding == null) {
                                    buffer = ByteBuffer.wrap(bytes);
                                } else if (ContentEncodings.LZ4.equals(contentEncoding)) {
                                    Parameter<Integer>.ParameterValue originalContentLength =
                                            HeaderUtil.get(ORIGINAL_CONTENT_LENGTH, request);
                                    if (originalContentLength.isError()) {
                                        tryComplete(HttpStatusCodes.BAD_REQUEST, callback);
                                        LOGGER.warn("Request has header Content-Encoding, but there is no valid Original-Content-Length");
                                        return;
                                    }
                                    buffer = decompressLz4(bytes, originalContentLength.get());
                                } else {
                                    tryComplete(HttpStatusCodes.UNSUPPORTED_MEDIA_TYPE, callback);
                                    return;
                                }

                                processingTimestamp = time.milliseconds();
                                sendAsync(this, buffer, callback);
                            } catch (RuntimeException ex) {
                                tryComplete(HttpStatusCodes.BAD_REQUEST, callback);
                                LOGGER.error("Cannot process request due to exception", ex);
                            } finally {
                                if (buffer != null) {
                                    ByteBufferPool.release(buffer);
                                }

                                cleanMDC();
                            }
                        }),
                        (r, exception) -> {
                            receivingEndedAtMs = time.milliseconds();

                            tryComplete(exception.getStatusCodeOrDefault(HttpStatusCodes.INTERNAL_SERVER_ERROR), callback);
                            LOGGER.error("Request body was read with exception", exception);
                        });
            } catch (Throwable throwable) {
                // Should never happened
                callback.call();
                LOGGER.error("Error on request body read full bytes", throwable);
                throw throwable;
            }
        }

        public void tryComplete(int code, Callback callback) {
            try {
                completionTimestamp = time.milliseconds();
                request.complete(code);
            } catch (Exception ex) {
                LOGGER.error("Error on request completion", ex);
            } finally {
                callback.call();
                metrics.update(this, code);
            }
        }

        public boolean isAsync() {
            return context.isAsync();
        }

        public ContentValidator validator() {
            return context.getValidator();
        }

        public long receivingTimeMs() {
            return Math.max(receivingEndedAtMs - receivingStartedAtMs, 0L);
        }

        public long decompressionTimeMs() {
            return decompressionTimeMs;
        }

        public long processingTimeMs() {
            return Math.max(completionTimestamp - processingTimestamp, 0L);
        }

        private ByteBuffer decompressLz4(byte[] bytes, int originalContentLength) {
            ByteBuffer buffer = ByteBufferPool.acquire(originalContentLength);
            decompressionTimeMs = TimeSource.SYSTEM.measureMs(() -> lz4Decompressor.decompress(bytes, buffer));
            return buffer;
        }

        private String getProtectedApiKey() {
            String apiKey = request.getHeader("apiKey");
            int pos = apiKey.lastIndexOf('_') + 1;
            if (pos > 0) {
                return StringUtil.mask(apiKey, '*', pos);
            }
            return StringUtil.mask(apiKey, '*', apiKey.length() / 2);
        }

        private void initMDC() {
            MDC.put("stream", context.getTopic());
            MDC.put("apiKey", getProtectedApiKey());
        }

        private void cleanMDC() {
            MDC.remove("stream");
            MDC.remove("apiKey");
        }
    }
}
