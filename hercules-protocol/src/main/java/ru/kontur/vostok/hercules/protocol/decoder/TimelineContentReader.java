package ru.kontur.vostok.hercules.protocol.decoder;

import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.TimelineContent;

public class TimelineContentReader implements Reader<TimelineContent> {

    private static final TimelineStateReader STATE_READER = new TimelineStateReader();
    private final ArrayReader<Event> eventArrayReader;

    public TimelineContentReader(Reader<Event> eventReader) {
        eventArrayReader = new ArrayReader<>(eventReader, Event.class);
    }

    @Override
    public TimelineContent read(Decoder decoder) {
        return new TimelineContent(
                STATE_READER.read(decoder),
                eventArrayReader.read(decoder)
        );
    }
}
