package ru.kontur.vostok.hercules.protocol.encoder;

import ru.kontur.vostok.hercules.protocol.TimelineState;
import ru.kontur.vostok.hercules.protocol.TimelineSliceState;

public class TimelineStateWriter implements Writer<TimelineState> {

    private static final ArrayWriter<TimelineSliceState> ARRAY_WRITER =
            new ArrayWriter<>(new TimelineSliceStateWriter());

    @Override
    public void write(Encoder encoder, TimelineState value) {
        ARRAY_WRITER.write(encoder, value.getSliceStates());
    }
}
