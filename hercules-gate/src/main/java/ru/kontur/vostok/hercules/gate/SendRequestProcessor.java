package ru.kontur.vostok.hercules.gate;

import com.codahale.metrics.Meter;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.gate.validation.EventValidator;
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
import ru.kontur.vostok.hercules.undertow.util.ResponseUtil;
import ru.kontur.vostok.hercules.util.logging.LoggingConstants;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Gregory Koshelev
 */
public class SendRequestProcessor implements RequestProcessor<HttpServerRequest, SendContext> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SendRequestProcessor.class);

    private static final Logger PROCESSED_EVENT_LOGGER = LoggerFactory.getLogger(LoggingConstants.PROCESSED_EVENT_LOGGER_NAME);
    private static final Logger DROPPED_EVENT_LOGGER = LoggerFactory.getLogger(LoggingConstants.DROPPED_EVENT_LOGGER_NAME);
    private static final Logger RECEIVED_EVENT_LOGGER = LoggerFactory.getLogger(LoggingConstants.RECEIVED_EVENT_LOGGER_NAME);

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
                                ReaderIterator<Event> reader;
                                try {
                                    reader = new ReaderIterator<>(new Decoder(bytes), EventReader.readTags(context.getTags()));
                                } catch (RuntimeException | InvalidDataException exception) {
                                    request.complete(HttpStatusCodes.BAD_REQUEST);
                                    callback.call();
                                    LOGGER.error("Cannot create ReaderIterator", exception);
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
        }
    }

    public void send(HttpServerRequest request, ReaderIterator<Event> reader, SendContext context, ThrottleCallback callback) {
        AtomicInteger pendingEvents = new AtomicInteger(reader.getTotal());
        AtomicBoolean processed = new AtomicBoolean(false);
        while (reader.hasNext()) {
            Event event;
            try {
                event = reader.next();
                RECEIVED_EVENT_LOGGER.trace("{}", event.getUuid());
                if (!eventValidator.validate(event)) {
                    //TODO: Metrics are coming!
                    LOGGER.warn("Invalid event data");
                    DROPPED_EVENT_LOGGER.trace("{}", event.getUuid());
                    if (processed.compareAndSet(false, true)) {
                        request.complete(HttpStatusCodes.BAD_REQUEST);
                        callback.call();
                    }
                    return;
                }
            } catch (Exception e) {
                LOGGER.error("Exception on validation event", e);
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
                DROPPED_EVENT_LOGGER.trace("{}", event.getUuid());
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
                        PROCESSED_EVENT_LOGGER.trace("{}", event.getUuid());
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
                        DROPPED_EVENT_LOGGER.trace("{}", event.getUuid());
                    }
            );
        }
        if (context.isAsync()) {
            request.complete(HttpStatusCodes.OK);
        }
    }
}
