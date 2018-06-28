package ru.kontur.vostok.hercules.protocol.encoder;

import ru.kontur.vostok.hercules.protocol.TimelineShardReadState;

public class TimelineShardReadStateWriter implements Writer<TimelineShardReadState> {

    private static final UUIDWriter uuidWriter = new UUIDWriter();

    @Override
    public void write(Encoder encoder, TimelineShardReadState value) {
        encoder.writeInteger(value.getShardId());
        encoder.writeLong(value.getTtOffset());
        uuidWriter.write(encoder, value.getEventId());
    }
}
