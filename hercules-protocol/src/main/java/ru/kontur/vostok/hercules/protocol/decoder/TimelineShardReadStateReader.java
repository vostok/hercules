package ru.kontur.vostok.hercules.protocol.decoder;

import ru.kontur.vostok.hercules.protocol.TimelineShardReadState;

public class TimelineShardReadStateReader implements Reader<TimelineShardReadState> {

    @Override
    public TimelineShardReadState read(Decoder decoder) {
        return new TimelineShardReadState(
                decoder.readInteger(),
                decoder.readLong(),
                decoder.readUuid()
        );
    }
}
