package ru.kontur.vostok.hercules.elastic.adapter.leprosery;

import ru.kontur.vostok.hercules.elastic.adapter.gate.GateSender;
import ru.kontur.vostok.hercules.protocol.Event;

import java.util.List;

/**
 * @author Gregory Koshelev
 */
public class LeproserySender {
    private final String stream;
    private final GateSender gateSender;

    public LeproserySender(String stream, GateSender gateSender) {
        this.stream = stream;
        this.gateSender = gateSender;
    }

    public void send(List<Event> events) {
        gateSender.send(events, true, stream);
    }
}
