package ru.kontur.vostok.hercules.protocol.decoder;

import ru.kontur.vostok.hercules.protocol.TimelineState;
import ru.kontur.vostok.hercules.protocol.TimelineSliceState;

public class TimelineStateReader implements Reader<TimelineState> {

    private static final ArrayReader<TimelineSliceState> ARRAY_READER =
            new ArrayReader<>(new TimelineSliceStateReader(), TimelineSliceState.class);

    @Override
    public TimelineState read(Decoder decoder) {
        return new TimelineState(ARRAY_READER.read(decoder));
    }
}
