package ru.kontur.vostok.hercules.protocol.decoder;

import ru.kontur.vostok.hercules.protocol.TimelineReadState;
import ru.kontur.vostok.hercules.protocol.TimelineShardReadState;

public class TimelineReadStateReader implements Reader<TimelineReadState> {

    private static final ArrayReader<TimelineShardReadState> arrayReader =
            new ArrayReader<>(new TimelineShardReadStateReader(), TimelineShardReadState.class);

    @Override
    public TimelineReadState read(Decoder decoder) {
        return new TimelineReadState(arrayReader.read(decoder));
    }
}
