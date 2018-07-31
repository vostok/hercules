package ru.kontur.vostok.hercules.protocol.decoder;

import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.TimelineContent;

public class TimelineContentReader implements Reader<TimelineContent> {

    private static final TimelineReadStateReader stateReader = new TimelineReadStateReader();
    private final ArrayReader<Event> eventArrayReader;

    public TimelineContentReader(Reader<Event> eventReader) {
        eventArrayReader = new ArrayReader<>(eventReader, Event.class);
    }

    @Override
    public TimelineContent read(Decoder decoder) {
        return new TimelineContent(
                stateReader.read(decoder),
                eventArrayReader.read(decoder)
        );
    }
}