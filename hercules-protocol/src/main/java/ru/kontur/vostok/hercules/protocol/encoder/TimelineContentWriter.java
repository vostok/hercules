package ru.kontur.vostok.hercules.protocol.encoder;

import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.TimelineContent;

public class TimelineContentWriter implements Writer<TimelineContent> {

    private static final TimelineReadStateWriter stateWriter = new TimelineReadStateWriter();
    private static final ArrayWriter<Event> arrayWriter = new ArrayWriter<>(new EventWriter());

    @Override
    public void write(Encoder encoder, TimelineContent value) {
        stateWriter.write(encoder, value.getReadState());
        arrayWriter.write(encoder, value.getEvents());
    }
}
