package ru.kontur.vostok.hercules.protocol.encoder;

import ru.kontur.vostok.hercules.protocol.EventId;

public class EventIdWriter implements Writer<EventId> {

    @Override
    public void write(Encoder encoder, EventId value) {
        encoder.writeLong(value.getP1());
        encoder.writeLong(value.getP2());
    }
}
