package ru.kontur.vostok.hercules.gate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import ru.kontur.vostok.hercules.gate.validation.EventValidator;
import ru.kontur.vostok.hercules.health.Meter;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.decoder.Decoder;
import ru.kontur.vostok.hercules.protocol.decoder.EventReader;
import ru.kontur.vostok.hercules.protocol.decoder.ReaderIterator;
import ru.kontur.vostok.hercules.protocol.decoder.exceptions.InvalidDataException;
import ru.kontur.vostok.hercules.throttling.RequestProcessor;
import ru.kontur.vostok.hercules.throttling.ThrottleCallback;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Gregory Koshelev
 */
public class SendRequestProcessor implements RequestProcessor<HttpServerRequest, SendContext> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SendRequestProcessor.class);

    private final EventSender eventSender;

    private final Meter sentEventsMeter;

    private final EventValidator eventValidator = new EventValidator();

    public SendRequestProcessor(MetricsCollector metricsCollector, EventSender eventSender) {
        this.eventSender = eventSender;

        this.sentEventsMeter = metricsCollector.meter(this.getClass().getSimpleName() + ".sentEvents");
    }

    @Override
    public void processAsync(HttpServerRequest request, SendContext context, ThrottleCallback callback) {
        try {
            request.readBodyAsync(
                    (r, bytes) -> request.dispatchAsync(
                            () -> {
                                initMDC(request, context);
                                ReaderIterator<Event> reader;
                                try {
                                    reader = new ReaderIterator<>(new Decoder(bytes), EventReader.readTags(context.getTags()));
                                } catch (RuntimeException | InvalidDataException ex) {
                                    request.complete(HttpStatusCodes.BAD_REQUEST);
                                    callback.call();
                                    LOGGER.error("Cannot create ReaderIterator", ex);
                                    return;
                                }
                                if (reader.getTotal() == 0) {
                                    request.complete(HttpStatusCodes.OK);
                                    callback.call();
                                    return;
                                }

                                send(request, reader, context, callback);
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
            callback.call();
            LOGGER.error("Error on request body read full bytes", throwable);
            throw throwable;
        } finally {
            cleanMDC();
        }
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
        MDC.put("topic",context.getTopic());
        MDC.put("apiKey", getProtectedApiKey(request));
    }

    private void cleanMDC() {
        MDC.remove("topic");
        MDC.remove("apiKey");
    }

    private String getProtectedApiKey(HttpServerRequest request) {
        String apiKey = request.getHeader("apiKey");
        return apiKey.substring(0, apiKey.lastIndexOf('_')) + "_*";
    }
}
