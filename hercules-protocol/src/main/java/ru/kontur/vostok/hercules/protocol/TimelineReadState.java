package ru.kontur.vostok.hercules.protocol;

public class TimelineReadState {

    private final TimelineShardReadState[] shards;

    public TimelineReadState(TimelineShardReadState[] shards) {
        this.shards = shards;
    }

    public TimelineShardReadState[] getShards() {
        return shards;
    }
}
