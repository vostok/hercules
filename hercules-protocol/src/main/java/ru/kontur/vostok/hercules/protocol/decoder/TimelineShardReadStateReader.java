package ru.kontur.vostok.hercules.protocol.decoder;

import ru.kontur.vostok.hercules.protocol.TimelineShardReadState;

public class TimelineShardReadStateReader implements Reader<TimelineShardReadState> {

    private static final UUIDReader EVENT_ID_READER = new UUIDReader();

    @Override
    public TimelineShardReadState read(Decoder decoder) {
        return new TimelineShardReadState(
                decoder.readInteger(),
                decoder.readLong(),
                EVENT_ID_READER.read(decoder)
        );
    }
}
