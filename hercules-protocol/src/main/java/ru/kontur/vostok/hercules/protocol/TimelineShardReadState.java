package ru.kontur.vostok.hercules.protocol;

import java.util.UUID;

public class TimelineShardReadState {

    private final int shardId;
    private final long eventTimestamp;
    private final UUID eventId;

    public TimelineShardReadState(int shardId, long eventTimestamp, UUID eventId) {
        this.shardId = shardId;
        this.eventTimestamp = eventTimestamp;
        this.eventId = eventId;
    }

    public int getShardId() {
        return shardId;
    }

    public long getEventTimestamp() {
        return eventTimestamp;
    }

    public UUID getEventId() {
        return eventId;
    }
}
