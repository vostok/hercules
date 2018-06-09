package ru.kontur.vostok.hercules.protocol.encoder;

import ru.kontur.vostok.hercules.protocol.Event;

public class EventWriter {

    public static void write(Encoder encoder, Event event) {
        encoder.writeRawBites(event.getBytes());
    }
}
