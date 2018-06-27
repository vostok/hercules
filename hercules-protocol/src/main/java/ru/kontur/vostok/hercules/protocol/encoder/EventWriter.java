package ru.kontur.vostok.hercules.protocol.encoder;

import ru.kontur.vostok.hercules.protocol.Event;

public class EventWriter implements Writer<Event> {

    @Override
    public void write(Encoder encoder, Event event) {
        encoder.writeRawBytes(event.getBytes());
    }
}
