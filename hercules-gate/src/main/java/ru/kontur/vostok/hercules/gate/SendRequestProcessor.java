package ru.kontur.vostok.hercules.gate;

import com.codahale.metrics.Meter;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.gate.validation.EventValidator;
import ru.kontur.vostok.hercules.metrics.MetricsCollector;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.decoder.Decoder;
import ru.kontur.vostok.hercules.protocol.decoder.EventReader;
import ru.kontur.vostok.hercules.protocol.decoder.ReaderIterator;
import ru.kontur.vostok.hercules.throttling.RequestProcessor;
import ru.kontur.vostok.hercules.throttling.ThrottleCallback;
import ru.kontur.vostok.hercules.undertow.util.ResponseUtil;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Gregory Koshelev
 */
public class SendRequestProcessor implements RequestProcessor<HttpServerExchange, SendContext> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SendRequestProcessor.class);

    private final EventSender eventSender;

    private final Meter sentEventsMeter;

    private final EventValidator eventValidator = new EventValidator();

    public SendRequestProcessor(MetricsCollector metricsCollector, EventSender eventSender) {
        this.eventSender = eventSender;

        this.sentEventsMeter = metricsCollector.meter(this.getClass().getSimpleName() + ".sent_events");
    }

    @Override
    public void processAsync(HttpServerExchange request, SendContext context, ThrottleCallback callback) {
        try {
            request.getRequestReceiver().receiveFullBytes(
                    (exchange, bytes) -> exchange.dispatch(() -> {
                        ReaderIterator<Event> reader;
                        try {
                            reader = new ReaderIterator<>(new Decoder(bytes), EventReader.readTags(context.tags));
                        } catch (Throwable throwable) {
                            ResponseUtil.badRequest(exchange);
                            callback.call();
                            throw throwable;//TODO: Process exception
                        }
                        if (reader.getTotal() == 0) {
                            ResponseUtil.ok(exchange);
                            callback.call();
                            return;
                        }

                        send(exchange, reader, context, callback);
                    }),
                    (exchange, e) -> {
                        try {
                            LOGGER.error("Request body was read with exception", e);
                            ResponseUtil.internalServerError(exchange);
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

    public void send(HttpServerExchange exchange, ReaderIterator<Event> reader, SendContext context, ThrottleCallback callback) {
        AtomicInteger pendingEvents = new AtomicInteger(reader.getTotal());
        AtomicBoolean processed = new AtomicBoolean(false);
        while (reader.hasNext()) {
            Event event;
            try {
                event = reader.next();
                if (!eventValidator.validate(event)) {
                    //TODO: Metrics are coming!
                    if (processed.compareAndSet(false, true)) {
                        ResponseUtil.badRequest(exchange);
                        callback.call();
                    }
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
                //TODO: Metrics are coming!
                if (processed.compareAndSet(false, true)) {
                    ResponseUtil.badRequest(exchange);
                    callback.call();
                }
                return;
            }
            if (!context.validator.validate(event)) {
                //TODO: should to log filtered events
                if (pendingEvents.decrementAndGet() == 0 && processed.compareAndSet(false, true)) {
                    if (!context.async) {
                        ResponseUtil.ok(exchange);
                    }
                    callback.call();
                }
                continue;
            }
            eventSender.send(
                    event,
                    event.getId(),
                    context.topic,
                    context.partitions,
                    context.shardingKey,
                    () -> {
                        if (pendingEvents.decrementAndGet() == 0 && processed.compareAndSet(false, true)) {
                            if (!context.async) {
                                ResponseUtil.ok(exchange);
                            }
                            callback.call();
                        }
                        sentEventsMeter.mark(1);
                    },
                    () -> {
                        //TODO: Metrics are coming!
                        if (processed.compareAndSet(false, true)) {
                            if (!context.async) {
                                ResponseUtil.unprocessableEntity(exchange);
                            }
                            callback.call();
                        }
                    });
        }
        if (context.async) {
            ResponseUtil.ok(exchange);
        }
    }
}
