package ru.kontur.vostok.hercules.protocol;

public class TimelineReadState {

    private final TimelineShardReadState[] shards;

    public TimelineReadState(TimelineShardReadState[] shards) {
        this.shards = shards;
    }

    public int getShardCount() {
        return shards.length;
    }

    public TimelineShardReadState[] getShards() {
        return shards;
    }
}
