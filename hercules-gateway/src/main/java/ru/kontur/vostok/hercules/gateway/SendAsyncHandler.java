package ru.kontur.vostok.hercules.gateway;

import io.undertow.server.HttpServerExchange;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.decoder.Decoder;
import ru.kontur.vostok.hercules.protocol.decoder.EventReader;
import ru.kontur.vostok.hercules.protocol.decoder.ReaderIterator;
import ru.kontur.vostok.hercules.uuid.Marker;

import java.util.Iterator;
import java.util.Set;

/**
 * @author Gregory Koshelev
 */
public class SendAsyncHandler extends GatewayHandler {
    public SendAsyncHandler(AuthManager authManager, EventSender eventSender, StreamRepository streamRepository) {
        super(authManager, eventSender, streamRepository);
    }

    @Override
    public void send(HttpServerExchange exchange, Marker marker, String topic, Set<String> tags, int partitions, String[] shardingKey) {
        exchange.getRequestReceiver().receiveFullBytes(
                (exch, bytes) -> {
                    exch.dispatch(() -> {
                        Iterator<Event> reader = new ReaderIterator<>(new Decoder(bytes), EventReader.readTags(tags));
                        while (reader.hasNext()) {
                            Event event = reader.next();
                            eventSender.send(
                                    event,
                                    uuidGenerator.next(marker),
                                    topic,
                                    partitions,
                                    shardingKey,
                                    null,
                                    null
                            );
                        }
                    });
                    exch.endExchange();
                });
    }
}
