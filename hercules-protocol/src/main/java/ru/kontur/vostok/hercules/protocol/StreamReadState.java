package ru.kontur.vostok.hercules.protocol;

public class StreamReadState {

    private final int shardCount;
    private final ShardReadState[] shardStates;

    public StreamReadState(int shardCount, ShardReadState[] shardStates) {
        this.shardCount = shardCount;
        this.shardStates = shardStates;
    }


}
