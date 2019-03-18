package ru.kontur.vostok.hercules.protocol;

public class StreamShardReadState {

    private final int partition;
    private final long offset;

    public StreamShardReadState(int partition, long offset) {
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
