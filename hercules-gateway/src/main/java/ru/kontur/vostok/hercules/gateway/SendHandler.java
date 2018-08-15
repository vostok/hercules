package ru.kontur.vostok.hercules.gateway;

import io.undertow.server.HttpServerExchange;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.metrics.MetricsCollector;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.decoder.Decoder;
import ru.kontur.vostok.hercules.protocol.decoder.EventReader;
import ru.kontur.vostok.hercules.protocol.decoder.ReaderIterator;
import ru.kontur.vostok.hercules.undertow.util.ResponseUtil;
import ru.kontur.vostok.hercules.uuid.Marker;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Gregory Koshelev
 */
public class SendHandler extends GatewayHandler {

    public SendHandler(MetricsCollector metricsCollector, AuthManager authManager, EventSender eventSender, StreamRepository streamRepository) {
        super(metricsCollector, authManager, eventSender, streamRepository);
    }

    @Override
    public void send(HttpServerExchange exchange, Marker marker, String topic, Set<String> tags, int partitions, String[] shardingKey) {
        exchange.getRequestReceiver().receiveFullBytes(
                (exch, bytes) -> exch.dispatch(() -> {
                    ReaderIterator<Event> reader = new ReaderIterator<>(new Decoder(bytes), EventReader.readTags(tags));
                    AtomicInteger pendingEvents = new AtomicInteger(reader.getTotal());
                    AtomicBoolean processed = new AtomicBoolean(false);
                    while (reader.hasNext()) {
                        Event event;
                        try {
                            event = reader.next();
                            if (!eventValidator.validate(event)) {
                                processed.set(true);
                                //TODO: Metrics are coming!
                                ResponseUtil.badRequest(exchange);
                                return;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            //TODO: Metrics are coming!
                            if (processed.compareAndSet(false, true)) {
                                ResponseUtil.badRequest(exchange);
                            }
                            return;
                        }
                        eventSender.send(
                                event,
                                event.getId(),
                                topic,
                                partitions,
                                shardingKey,
                                () -> {
                                    if (pendingEvents.decrementAndGet() == 0 && processed.compareAndSet(false, true)) {
                                        ResponseUtil.ok(exchange);
                                    }
                                    sentEventsMeter.mark(1);
                                },
                                () -> {
                                    //TODO: Metrics are coming!
                                    if (processed.compareAndSet(false, true)) {
                                        ResponseUtil.unprocessableEntity(exchange);
                                    }
                                });
                    }
                }));
    }
}
