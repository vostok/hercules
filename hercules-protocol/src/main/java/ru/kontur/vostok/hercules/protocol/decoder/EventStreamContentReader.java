package ru.kontur.vostok.hercules.protocol.decoder;

import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventStreamContent;

public class EventStreamContentReader implements Reader<EventStreamContent> {

    private static final StreamReadStateReader STATE_READER = new StreamReadStateReader();
    private static final ArrayReader<Event> ARRAY_READER = new ArrayReader<>(EventReader.readAllTags(), Event.class);

    @Override
    public EventStreamContent read(Decoder decoder) {
        return new EventStreamContent(
                STATE_READER.read(decoder),
                ARRAY_READER.read(decoder)
        );
    }
}
