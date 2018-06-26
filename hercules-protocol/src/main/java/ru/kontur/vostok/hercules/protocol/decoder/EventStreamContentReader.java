package ru.kontur.vostok.hercules.protocol.decoder;

import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventStreamContent;

public class EventStreamContentReader implements Reader<EventStreamContent> {

    private static final StreamReadStateReader stateReader = new StreamReadStateReader();
    private static final ArrayReader<Event> arrayReader = new ArrayReader<>(EventReader.readAllTags(), Event.class);

    @Override
    public EventStreamContent read(Decoder decoder) {
        return new EventStreamContent(
                stateReader.read(decoder),
                arrayReader.read(decoder)
        );
    }
}
