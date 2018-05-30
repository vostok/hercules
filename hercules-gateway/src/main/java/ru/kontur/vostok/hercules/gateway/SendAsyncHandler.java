package ru.kontur.vostok.hercules.gateway;

import io.undertow.server.HttpServerExchange;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventReader;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;

import java.util.Set;

/**
 * @author Gregory Koshelev
 */
public class SendAsyncHandler extends GatewayHandler {
    public SendAsyncHandler(AuthManager authManager, EventSender eventSender, StreamRepository streamRepository) {
        super(authManager, eventSender, streamRepository);
    }

    @Override
    public void send(HttpServerExchange exchange, String topic, Set<String> tags, int partitions, String[] shardingKey) {
        exchange.getRequestReceiver().receiveFullBytes(
                (exch, bytes) -> {
                    exch.dispatch(() -> {
                        EventReader reader = new EventReader(bytes, tags);
                        while (reader.hasNext()) {
                            Event event = reader.read();
                            eventSender.send(event, topic, partitions, shardingKey, null, null);
                        }
                    });
                    exch.endExchange();
                });
    }
}
