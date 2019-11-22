package ru.kontur.vostok.hercules.protocol;

public class StreamShardReadState {
    private static final int SIZE_OF_PARTITION = Sizes.SIZE_OF_INTEGER;
    private static final int SIZE_OF_OFFSET = Sizes.SIZE_OF_LONG;

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
