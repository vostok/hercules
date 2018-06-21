package ru.kontur.vostok.hercules.protocol.encoder;


import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventStreamContent;

public class EventStreamContentWriter implements Writer<EventStreamContent> {

    private static final StreamReadStateWriter STREAM_READ_STATE_WRITER = new StreamReadStateWriter();
    private static final ArrayWriter<Event> EVENT_ARRAY_WRITER = new ArrayWriter<>(new EventWriter());

    public void write(Encoder encoder, EventStreamContent content) {
        STREAM_READ_STATE_WRITER.write(encoder, content.getState());
        EVENT_ARRAY_WRITER.write(encoder, content.getEvents());
    }
}
