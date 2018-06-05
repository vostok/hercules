package ru.kontur.vostok.hercules.protocol;

public class ShardReadState {
    private final int partition;
    private final long offset;

    public ShardReadState(int partition, long offset) {
        this.partition = partition;
        this.offset = offset;
    }

    public int getPartition() {
        return partition;
    }

    public long getOffset() {
        return offset;
    }
}
