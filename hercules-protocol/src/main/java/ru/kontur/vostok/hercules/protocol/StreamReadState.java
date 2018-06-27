package ru.kontur.vostok.hercules.protocol;

public class StreamReadState {

    private final int shardCount;
    private final StreamShardReadState[] shardStates;

    public StreamReadState(StreamShardReadState[] shardStates) {
        this.shardCount = shardStates.length;
        this.shardStates = shardStates;
    }

    public int getShardCount() {
        return shardCount;
    }

    public StreamShardReadState[] getShardStates() {
        return shardStates;
    }
}
