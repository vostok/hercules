package ru.kontur.vostok.hercules.gate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import ru.kontur.vostok.hercules.gate.validation.EventValidator;
import ru.kontur.vostok.hercules.health.AutoMetricStopwatch;
import ru.kontur.vostok.hercules.health.Histogram;
import ru.kontur.vostok.hercules.health.Meter;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.health.Timer;
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
import ru.kontur.vostok.hercules.throttling.RequestProcessor;
import ru.kontur.vostok.hercules.throttling.ThrottleCallback;
import ru.kontur.vostok.hercules.util.ByteBufferPool;
import ru.kontur.vostok.hercules.util.compression.Lz4Decompressor;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.text.StringUtil;
import ru.kontur.vostok.hercules.util.time.TimeSource;
import ru.kontur.vostok.hercules.util.validation.IntegerValidators;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Gregory Koshelev
 */
public class SendRequestProcessor implements RequestProcessor<HttpServerRequest, SendContext> {
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

    private final Timer recvTimeMsTimer;
    private final Timer decompressionTimeMsTimer;
    private final Timer processingTimeMsTimer;
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

        this.recvTimeMsTimer = metricsCollector.timer(this.getClass().getSimpleName() + ".recvTimeMs");
        this.decompressionTimeMsTimer = metricsCollector.timer(this.getClass().getSimpleName() + ".decompressionTimeMs");
        this.processingTimeMsTimer = metricsCollector.timer(this.getClass().getSimpleName() + ".processingTimeMs");
        this.sentEventsMeter = metricsCollector.meter(this.getClass().getSimpleName() + ".sentEvents");
        this.lostEventsMeter = metricsCollector.meter(this.getClass().getSimpleName() + ".lostEvents");
        this.recvEventsHistogram = metricsCollector.histogram(this.getClass().getSimpleName() + ".recvEvents");
        this.eventSizeHistogram = metricsCollector.histogram(this.getClass().getSimpleName() + ".eventSize");
    }

    @Override
    public void processAsync(HttpServerRequest request, SendContext context, ThrottleCallback callback) {
        try {
            final long recvStartedAtMs = time.milliseconds();
            request.readBodyAsync(
                    (r, bytes) -> request.dispatchAsync(
                            () -> {
                                recvTimeMsTimer.update(time.milliseconds() - recvStartedAtMs);

                                ByteBuffer buffer = null;
                                try {
                                    initMDC(request, context);

                                    String contentEncoding = request.getHeader(HttpHeaders.CONTENT_ENCODING);
                                    if (contentEncoding == null) {
                                        buffer = ByteBuffer.wrap(bytes);
                                    } else if (ContentEncodings.LZ4.equals(contentEncoding)) {
                                        Parameter<Integer>.ParameterValue originalContentLength =
                                                HeaderUtil.get(ORIGINAL_CONTENT_LENGTH, request);
                                        if (originalContentLength.isError()) {
                                            tryComplete(request, HttpStatusCodes.BAD_REQUEST, callback);
                                            LOGGER.warn("Request has header Content-Encoding, but there is no valid Original-Content-Length");
                                            return;
                                        }
                                        buffer = ByteBufferPool.acquire(originalContentLength.get());
                                        try (AutoMetricStopwatch ignored = new AutoMetricStopwatch(decompressionTimeMsTimer, TimeUnit.MILLISECONDS, time)) {
                                            lz4Decompressor.decompress(bytes, buffer);
                                        }
                                    } else {
                                        tryComplete(request, HttpStatusCodes.UNSUPPORTED_MEDIA_TYPE, callback);
                                        return;
                                    }

                                    try (AutoMetricStopwatch ignored = new AutoMetricStopwatch(processingTimeMsTimer, TimeUnit.MILLISECONDS, time)) {
                                        process(request, buffer, context, callback);
                                    }
                                } catch (RuntimeException ex) {
                                    tryComplete(request, HttpStatusCodes.BAD_REQUEST, callback);
                                    LOGGER.error("Cannot process request due to exception", ex);
                                } finally {
                                    if (buffer != null) {
                                        ByteBufferPool.release(buffer);
                                    }

                                    cleanMDC();
                                }
                            }),
                    (r, e) -> {
                        tryComplete(request, e.getStatusCodeOrDefault(HttpStatusCodes.INTERNAL_SERVER_ERROR), callback);
                        LOGGER.error("Request body was read with exception", e);
                    });
        } catch (Throwable throwable) {
            // Should never happened
            callback.call();
            LOGGER.error("Error on request body read full bytes", throwable);
            throw throwable;
        }

    }

    private void process(HttpServerRequest request, ByteBuffer buffer, SendContext context, ThrottleCallback callback) {
        ReaderIterator<Event> reader;
        try {
            reader = new ReaderIterator<>(new Decoder(buffer), EventReader.readTags(context.getTags()));
        } catch (InvalidDataException ex) {
            tryComplete(request, HttpStatusCodes.BAD_REQUEST, callback);
            LOGGER.error("Request is malformed", ex);
            return;
        }
        if (reader.getTotal() == 0) {
            tryComplete(request, HttpStatusCodes.OK, callback);
            return;
        }
        send(request, reader, context, callback);
    }

    private void send(HttpServerRequest request, ReaderIterator<Event> reader, SendContext context, ThrottleCallback callback) {
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
                        tryComplete(request, HttpStatusCodes.BAD_REQUEST, callback);
                    }
                    //TODO: Metrics are coming!
                    LOGGER.warn("Invalid event data");
                    return;
                }
            } catch (Exception ex) {
                if (processed.compareAndSet(false, true)) {
                    tryComplete(request, HttpStatusCodes.BAD_REQUEST, callback);
                }
                LOGGER.error("Exception on validation event", ex);
                //TODO: Metrics are coming!
                return;
            }
            if (!context.getValidator().validate(event)) {
                //TODO: should to log filtered events
                if (pendingEvents.decrementAndGet() == 0 && processed.compareAndSet(false, true)) {
                    if (!context.isAsync()) {
                        tryComplete(request, HttpStatusCodes.OK, callback);
                    } else {
                        callback.call();
                    }
                }
                continue;
            }
            eventSender.send(
                    event,
                    event.getUuid(),//TODO: Think hard about this!
                    context.getTopic(),
                    context.getPartitions(),
                    context.getShardingKey(),
                    () -> {
                        if (pendingEvents.decrementAndGet() == 0 && processed.compareAndSet(false, true)) {
                            if (!context.isAsync()) {
                                tryComplete(request, HttpStatusCodes.OK, callback);
                            } else {
                                callback.call();
                            }
                        }
                        sentEventsMeter.mark();
                    },
                    () -> {
                        if (processed.compareAndSet(false, true)) {
                            if (!context.isAsync()) {
                                tryComplete(request, HttpStatusCodes.INTERNAL_SERVER_ERROR, callback);
                            } else {
                                callback.call();
                            }
                        }
                        lostEventsMeter.mark();
                    }
            );
        }
        if (context.isAsync()) {
            request.complete(HttpStatusCodes.OK);
        }
    }

    private void tryComplete(HttpServerRequest request, int code, ThrottleCallback callback) {
        try {
            request.complete(code);
        } catch (Exception ex) {
            LOGGER.error("Error on request completion", ex);
        } finally {
            callback.call();
        }
    }

    private void initMDC(HttpServerRequest request, SendContext context) {
        MDC.put("topic", context.getTopic());
        MDC.put("apiKey", getProtectedApiKey(request));
    }

    private void cleanMDC() {
        MDC.remove("topic");
        MDC.remove("apiKey");
    }

    private String getProtectedApiKey(HttpServerRequest request) {
        String apiKey = request.getHeader("apiKey");
        int pos = apiKey.lastIndexOf('_') + 1;
        if (pos > 0) {
            return StringUtil.mask(apiKey, '*', pos);
        }
        return StringUtil.mask(apiKey, '*', apiKey.length() / 2);
    }
}
