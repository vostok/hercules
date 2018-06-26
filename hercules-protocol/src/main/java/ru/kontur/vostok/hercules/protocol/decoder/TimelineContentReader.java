package ru.kontur.vostok.hercules.protocol.decoder;

import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.TimelineContent;

public class TimelineContentReader implements Reader<TimelineContent> {

    private static final TimelineReadStateReader STATE_READER = new TimelineReadStateReader();
    private final ArrrayReader<Event> eventArrrayReader;

    public TimelineContentReader(Reader<Event> eventReader) {
        eventArrrayReader = new ArrrayReader<>(eventReader, Event.class);
    }

    @Override
    public TimelineContent read(Decoder decoder) {
        return new TimelineContent(
                STATE_READER.read(decoder),
                eventArrrayReader.read(decoder)
        );
    }
}
