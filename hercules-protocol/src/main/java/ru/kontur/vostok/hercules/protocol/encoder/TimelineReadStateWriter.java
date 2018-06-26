package ru.kontur.vostok.hercules.protocol.encoder;

import ru.kontur.vostok.hercules.protocol.TimelineReadState;
import ru.kontur.vostok.hercules.protocol.TimelineShardReadState;

public class TimelineReadStateWriter implements Writer<TimelineReadState> {

    private static final ArrayWriter<TimelineShardReadState> arrayWriter =
            new ArrayWriter<>(new TimelineShardReadStateWriter());

    @Override
    public void write(Encoder encoder, TimelineReadState value) {
        arrayWriter.write(encoder, value.getShards());
    }
}
