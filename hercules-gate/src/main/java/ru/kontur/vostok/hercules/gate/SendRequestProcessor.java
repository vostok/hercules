package ru.kontur.vostok.hercules.gate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import ru.kontur.vostok.hercules.gate.validation.EventValidator;
import ru.kontur.vostok.hercules.health.AutoMetricStopwatch;
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
import ru.kontur.vostok.hercules.util.parameter.ParameterValue;
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

    private final EventValidator eventValidator = new EventValidator();
    private final Lz4Decompressor lz4Decompressor = new Lz4Decompressor();

    private final Timer readEventsDurationMsTimer;
    private final Meter sentEventsMeter;
    private final Timer decompressionTimeMsTimer;

    public SendRequestProcessor(EventSender eventSender, MetricsCollector metricsCollector) {
        this(eventSender, metricsCollector, TimeSource.SYSTEM);
    }

    SendRequestProcessor(EventSender eventSender, MetricsCollector metricsCollector, TimeSource time) {
        this.eventSender = eventSender;
        this.time = time;

        this.readEventsDurationMsTimer = metricsCollector.timer(this.getClass().getSimpleName() + ".readEventsDurationMs");
        this.sentEventsMeter = metricsCollector.meter(this.getClass().getSimpleName() + ".sentEvents");
        this.decompressionTimeMsTimer = metricsCollector.timer(this.getClass().getSimpleName() + ".decompressionTimeMs");
    }

    @Override
    public void processAsync(HttpServerRequest request, SendContext context, ThrottleCallback callback) {
        try {
            final long readEventsStartedAt = time.milliseconds();
            request.readBodyAsync(
                    (r, bytes) -> request.dispatchAsync(
                            () -> {
                                readEventsDurationMsTimer.update(time.milliseconds() - readEventsStartedAt);

                                ByteBuffer buffer = null;
                                try {
                                    initMDC(request, context);

                                    String contentEncoding = request.getHeader(HttpHeaders.CONTENT_ENCODING);
                                    if (contentEncoding == null) {
                                        buffer = ByteBuffer.wrap(bytes);
                                    } else if (ContentEncodings.LZ4.equals(contentEncoding)) {
                                        ParameterValue<Integer> originalContentLength =
                                                HeaderUtil.get(ORIGINAL_CONTENT_LENGTH, request);
                                        if (originalContentLength.isError()) {
                                            LOGGER.warn("Request has header Content-Encoding, but there is no valid Original-Content-Length");
                                            request.complete(HttpStatusCodes.BAD_REQUEST);
                                            callback.call();
                                            return;
                                        }
                                        buffer = ByteBufferPool.acquire(originalContentLength.get());
                                        try (AutoMetricStopwatch ignored = new AutoMetricStopwatch(decompressionTimeMsTimer, TimeUnit.MILLISECONDS, time)) {
                                            lz4Decompressor.decompress(bytes, buffer);
                                        }
                                    } else {
                                        request.complete(HttpStatusCodes.UNSUPPORTED_MEDIA_TYPE);
                                        callback.call();
                                        return;
                                    }

                                    process(request, buffer, context, callback);
                                } catch (RuntimeException ex) {
                                    request.complete(HttpStatusCodes.BAD_REQUEST);
                                    LOGGER.error("Cannot process request due to exception", ex);
                                    callback.call();
                                } finally {
                                    if (buffer != null) {
                                        ByteBufferPool.release(buffer);
                                    }

                                    cleanMDC();
                                }
                            }),
                    (r, e) -> {
                        try {
                            LOGGER.error("Request body was read with exception", e);
                            request.complete(HttpStatusCodes.INTERNAL_SERVER_ERROR);
                        } finally {
                            callback.call();
                        }
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
            request.complete(HttpStatusCodes.BAD_REQUEST);
            callback.call();
            LOGGER.error("Request is malformed", ex);
            return;
        }
        if (reader.getTotal() == 0) {
            request.complete(HttpStatusCodes.OK);
            callback.call();
            return;
        }
        send(request, reader, context, callback);
    }

    private void send(HttpServerRequest request, ReaderIterator<Event> reader, SendContext context, ThrottleCallback callback) {
        AtomicInteger pendingEvents = new AtomicInteger(reader.getTotal());
        AtomicBoolean processed = new AtomicBoolean(false);
        while (reader.hasNext()) {
            Event event;
            try {
                event = reader.next();
                if (!eventValidator.validate(event)) {
                    //TODO: Metrics are coming!
                    LOGGER.warn("Invalid event data");
                    if (processed.compareAndSet(false, true)) {
                        request.complete(HttpStatusCodes.BAD_REQUEST);
                        callback.call();
                    }
                    return;
                }
            } catch (Exception ex) {
                LOGGER.error("Exception on validation event", ex);
                //TODO: Metrics are coming!
                if (processed.compareAndSet(false, true)) {
                    request.complete(HttpStatusCodes.BAD_REQUEST);
                    callback.call();
                }
                return;
            }
            if (!context.getValidator().validate(event)) {
                //TODO: should to log filtered events
                if (pendingEvents.decrementAndGet() == 0 && processed.compareAndSet(false, true)) {
                    if (!context.isAsync()) {
                        request.complete(HttpStatusCodes.OK);
                    }
                    callback.call();
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
                                request.complete(HttpStatusCodes.OK);
                            }
                            callback.call();
                        }
                        sentEventsMeter.mark(1);
                    },
                    () -> {
                        //TODO: Metrics are coming!
                        if (processed.compareAndSet(false, true)) {
                            if (!context.isAsync()) {
                                request.complete(HttpStatusCodes.INTERNAL_SERVER_ERROR);
                            }
                            callback.call();
                        }
                    }
            );
        }
        if (context.isAsync()) {
            request.complete(HttpStatusCodes.OK);
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
