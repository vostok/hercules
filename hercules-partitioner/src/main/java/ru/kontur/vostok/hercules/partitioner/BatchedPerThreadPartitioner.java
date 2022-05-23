package ru.kontur.vostok.hercules.partitioner;

import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.util.number.IntegerUtil;
import ru.kontur.vostok.hercules.util.random.RandomUtil;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Partitioner returns the same partition number for batch of events when it is called from the same thread.
 *
 * @author Gregory Koshelev
 */
public class BatchedPerThreadPartitioner implements Partitioner {
    private final ThreadLocal<AtomicInteger> value;
    private final int batchSize;

    /**
     * Visible for testing.
     *
     * @param batchSize batch size
     * @param supplier supplies {@code AtomicInteger} seed
     */
    BatchedPerThreadPartitioner(int batchSize, Supplier<AtomicInteger> supplier) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("Batch size must be positive");
        }

        value = ThreadLocal.withInitial(supplier);
        this.batchSize = batchSize;
    }

    public BatchedPerThreadPartitioner(int batchSize) {
        this(batchSize, () -> new AtomicInteger(RandomUtil.generateIntSeed()));
    }

    @Override
    public int partition(Event event, ShardingKey shardingKey, int partitions) {
        return (IntegerUtil.toPositive(value.get().getAndIncrement()) / batchSize) % partitions;
    }
}
