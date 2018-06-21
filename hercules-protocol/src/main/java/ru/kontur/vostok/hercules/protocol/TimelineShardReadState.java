package ru.kontur.vostok.hercules.protocol;

public class TimelineShardReadState {

    private final int shardId;
    private final long eventTimestamp;
    private final EventId eventId;

    public TimelineShardReadState(int shardId, long eventTimestamp, EventId eventId) {
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

    public EventId getEventId() {
        return eventId;
    }
}
