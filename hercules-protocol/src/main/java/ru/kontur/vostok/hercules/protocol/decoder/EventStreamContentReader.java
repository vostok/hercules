package ru.kontur.vostok.hercules.protocol.decoder;

import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventStreamContent;
import ru.kontur.vostok.hercules.protocol.StreamReadState;

public class EventStreamContentReader implements Reader<EventStreamContent> {

    private static final StreamReadStateReader STREAM_READ_STATE_READER = new StreamReadStateReader();

    @Override
    public EventStreamContent read(Decoder decoder) {
        StreamReadState readState = STREAM_READ_STATE_READER.read(decoder);
        int count = decoder.readInteger();
        EventReader eventReader = EventReader.batchReaderWithCount(decoder, count);
        Event[] records = new Event[count];
        for (int i = 0; i < count; ++i) {
            records[i] = eventReader.read();
        }
        return new EventStreamContent(readState, records);
    }
}
