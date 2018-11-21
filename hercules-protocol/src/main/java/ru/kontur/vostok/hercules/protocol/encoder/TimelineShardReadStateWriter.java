package ru.kontur.vostok.hercules.protocol.encoder;

import ru.kontur.vostok.hercules.protocol.TimelineShardReadState;

public class TimelineShardReadStateWriter implements Writer<TimelineShardReadState> {

    private static final UUIDWriter UUID_WRITER = new UUIDWriter();

    @Override
    public void write(Encoder encoder, TimelineShardReadState value) {
        encoder.writeInteger(value.getShardId());
        encoder.writeLong(value.getTtOffset());
        UUID_WRITER.write(encoder, value.getEventId());
    }
}
