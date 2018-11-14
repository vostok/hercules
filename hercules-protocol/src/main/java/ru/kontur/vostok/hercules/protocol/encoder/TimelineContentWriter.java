package ru.kontur.vostok.hercules.protocol.encoder;

import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.TimelineContent;

public class TimelineContentWriter implements Writer<TimelineContent> {

    private static final TimelineReadStateWriter STATE_WRITER = new TimelineReadStateWriter();
    private static final ArrayWriter<Event> ARRAY_WRITER = new ArrayWriter<>(new EventWriter());

    @Override
    public void write(Encoder encoder, TimelineContent value) {
        STATE_WRITER.write(encoder, value.getReadState());
        ARRAY_WRITER.write(encoder, value.getEvents());
    }
}
