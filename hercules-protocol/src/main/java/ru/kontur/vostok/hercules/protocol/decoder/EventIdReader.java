package ru.kontur.vostok.hercules.protocol.decoder;

import ru.kontur.vostok.hercules.protocol.EventId;

public class EventIdReader implements Reader<EventId> {

    @Override
    public EventId read(Decoder decoder) {
        return new EventId(decoder.readLong(), decoder.readLong());
    }
}
