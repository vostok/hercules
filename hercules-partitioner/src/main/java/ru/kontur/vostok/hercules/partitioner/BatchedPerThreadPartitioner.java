package ru.kontur.vostok.hercules.partitioner;

import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.util.number.IntegerUtil;
import ru.kontur.vostok.hercules.util.random.RandomUtil;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Partitioner returns the same partition number for batch of events when it is called from the same thread.
 *
 * @author Gregory Koshelev
 */
public class BatchedPerThreadPartitioner implements Partitioner {
    private final ThreadLocal<AtomicInteger> value =
            ThreadLocal.withInitial(() -> new AtomicInteger(RandomUtil.generateIntSeed()));
    private final int batchSize;

    public BatchedPerThreadPartitioner(int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("Batch size must be positive");
        }

        this.batchSize = batchSize;
    }

    @Override
    public int partition(Event event, ShardingKey shardingKey, int partitions) {
        return (IntegerUtil.toPositive(value.get().getAndIncrement()) / batchSize) % partitions;
    }
}
