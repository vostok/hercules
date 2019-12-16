package ru.kontur.vostok.hercules.protocol;

public class StreamShardReadState {
    private static final int SIZE_OF_PARTITION = Type.INTEGER.size;
    private static final int SIZE_OF_OFFSET = Type.LONG.size;

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

    public int sizeOf() {
        return fixedSizeOf();
    }

    public static int fixedSizeOf() {
        return SIZE_OF_PARTITION + SIZE_OF_OFFSET;
    }
}
