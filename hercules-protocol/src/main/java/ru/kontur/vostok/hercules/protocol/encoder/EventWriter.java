package ru.kontur.vostok.hercules.protocol.encoder;

import ru.kontur.vostok.hercules.protocol.Event;

public class EventWriter {

    public static void write(Encoder encoder, Event event) {
        for (byte b : event.getBytes()) {
            encoder.writeByte(b);
        }
    }
}
