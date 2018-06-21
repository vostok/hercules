package ru.kontur.vostok.hercules.protocol.decoder;

import ru.kontur.vostok.hercules.protocol.TimelineShardReadState;

public class TimelineShardReadStateReader implements Reader<TimelineShardReadState> {

    private static final EventIdReader EVENT_ID_READER = new EventIdReader();

    @Override
    public TimelineShardReadState read(Decoder decoder) {
        return new TimelineShardReadState(
                decoder.readInteger(),
                decoder.readLong(),
                EVENT_ID_READER.read(decoder)
        );
    }
}
