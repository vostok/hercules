package ru.kontur.vostok.hercules.protocol.encoder;

import ru.kontur.vostok.hercules.protocol.TimelineShardReadState;

public class TimelineShardReadStateWriter implements Writer<TimelineShardReadState> {

    @Override
    public void write(Encoder encoder, TimelineShardReadState value) {
        encoder.writeInteger(value.getShardId());
        encoder.writeLong(value.getTtOffset());
        encoder.writeUuid(value.getEventId());
    }
}
