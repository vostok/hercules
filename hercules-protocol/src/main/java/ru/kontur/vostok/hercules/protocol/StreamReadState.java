package ru.kontur.vostok.hercules.protocol;

public class StreamReadState {

    private final StreamShardReadState[] shardStates;

    public StreamReadState(StreamShardReadState[] shardStates) {
        this.shardStates = shardStates;
    }

    public int getShardCount() {
        return shardStates.length;
    }

    public StreamShardReadState[] getShardStates() {
        return shardStates;
    }
}
