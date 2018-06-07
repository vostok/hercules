package ru.kontur.vostok.hercules.protocol.encoder;


import ru.kontur.vostok.hercules.protocol.EventStreamContent;

public class EventStreamContentWriter {

    public static void write(Encoder encoder, EventStreamContent content) {
        StreamReadStateWriter.write(encoder, content.getState());
        encoder.writeInteger(content.getEventCount());
        for (String record : content.getEvents()) {
            encoder.writeText(record);
        }
    }
}
