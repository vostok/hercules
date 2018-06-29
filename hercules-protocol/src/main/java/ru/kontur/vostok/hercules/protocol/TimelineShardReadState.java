package ru.kontur.vostok.hercules.protocol;

import java.util.UUID;

public class TimelineShardReadState {

    private final int shardId;
    private final long ttOffset;
    private final UUID eventId;

    public TimelineShardReadState(int shardId, long ttOffset, UUID eventId) {
        this.shardId = shardId;
        this.ttOffset = ttOffset;
        this.eventId = eventId;
    }

    public int getShardId() {
        return shardId;
    }

    public long getTtOffset() {
        return ttOffset;
    }

    public UUID getEventId() {
        return eventId;
    }
}
