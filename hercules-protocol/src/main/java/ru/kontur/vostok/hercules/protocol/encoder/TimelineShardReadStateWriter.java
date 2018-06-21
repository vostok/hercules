package ru.kontur.vostok.hercules.protocol.encoder;

import ru.kontur.vostok.hercules.protocol.TimelineShardReadState;

public class TimelineShardReadStateWriter implements Writer<TimelineShardReadState> {

    private static final EventIdWriter EVENT_ID_WRITER = new EventIdWriter();

    @Override
    public void write(Encoder encoder, TimelineShardReadState value) {
        encoder.writeInteger(value.getShardId());
        encoder.writeLong(value.getEventTimestamp());
        EVENT_ID_WRITER.write(encoder, value.getEventId());
    }
}
