package ru.kontur.vostok.hercules.protocol;

public class StreamReadState {
    private static int SIZE_OF_SHARD_STATE_COUNT = Type.INTEGER.size;

    private final StreamShardReadState[] shardStates;
    private final int size;

    public StreamReadState(StreamShardReadState[] shardStates) {
        this.shardStates = shardStates;

        this.size = SIZE_OF_SHARD_STATE_COUNT + shardStates.length * StreamShardReadState.fixedSizeOf();
    }

    public int getShardCount() {
        return shardStates.length;
    }

    public StreamShardReadState[] getShardStates() {
        return shardStates;
    }

    public int sizeOf() {
        return size;
    }
}
