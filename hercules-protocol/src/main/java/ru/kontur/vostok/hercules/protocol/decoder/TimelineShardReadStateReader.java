package ru.kontur.vostok.hercules.protocol.decoder;

import ru.kontur.vostok.hercules.protocol.TimelineShardReadState;

public class TimelineShardReadStateReader implements Reader<TimelineShardReadState> {

    private static final UUIDReader UUID_READER = new UUIDReader();

    @Override
    public TimelineShardReadState read(Decoder decoder) {
        return new TimelineShardReadState(
                decoder.readInteger(),
                decoder.readLong(),
                UUID_READER.read(decoder)
        );
    }
}
