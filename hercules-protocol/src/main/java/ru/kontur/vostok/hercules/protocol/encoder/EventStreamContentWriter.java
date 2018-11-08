package ru.kontur.vostok.hercules.protocol.encoder;


import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventStreamContent;

public class EventStreamContentWriter implements Writer<EventStreamContent> {

    private static final StreamReadStateWriter STATE_WRITER = new StreamReadStateWriter();
    private static final ArrayWriter<Event> ARRAY_WRITER = new ArrayWriter<>(new EventWriter());

    public void write(Encoder encoder, EventStreamContent content) {
        STATE_WRITER.write(encoder, content.getState());
        ARRAY_WRITER.write(encoder, content.getEvents());
    }
}
