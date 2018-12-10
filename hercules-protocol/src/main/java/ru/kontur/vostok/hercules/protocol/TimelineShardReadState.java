package ru.kontur.vostok.hercules.protocol;

public class TimelineShardReadState {

    private final int shardId;
    private final long ttOffset;
    private final byte[] eventId;

    public TimelineShardReadState(int shardId, long ttOffset, byte[] eventId) {
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

    public byte[] getEventId() {
        return eventId;
    }
}
