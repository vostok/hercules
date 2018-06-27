package ru.kontur.vostok.hercules.protocol.encoder;


import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventStreamContent;

public class EventStreamContentWriter implements Writer<EventStreamContent> {

    private static final StreamReadStateWriter stateWriter = new StreamReadStateWriter();
    private static final ArrayWriter<Event> arrayWriter = new ArrayWriter<>(new EventWriter());

    public void write(Encoder encoder, EventStreamContent content) {
        stateWriter.write(encoder, content.getState());
        arrayWriter.write(encoder, content.getEvents());
    }
}
