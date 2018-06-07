package ru.kontur.vostok.hercules.protocol;

public class StreamReadState {

    private final int shardCount;
    private final ShardReadState[] shardStates;

    public StreamReadState(ShardReadState[] shardStates) {
        this.shardCount = shardStates.length;
        this.shardStates = shardStates;
    }

    public int getShardCount() {
        return shardCount;
    }

    public ShardReadState[] getShardStates() {
        return shardStates;
    }
}
