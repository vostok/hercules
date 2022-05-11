package ru.kontur.vostok.hercules.partitioner;

import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.util.random.RandomUtil;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Partitioner provides partition numbers which almost uniformly distributed.
 * <p>
 * Round robin algorithm is used internally.
 *
 * @author Gregory Koshelev
 */
public class RoundRobinPartitioner implements Partitioner {
    private final AtomicInteger value;

    /**
     * Visible for testing.
     *
     * @param seed random seed
     */
    RoundRobinPartitioner(int seed) {
        this.value = new AtomicInteger(seed);
    }

    public RoundRobinPartitioner() {
        this(RandomUtil.generateIntSeed());
    }

    @Override
    public int partition(Event event, ShardingKey shardingKey, int partitions) {
        return Integer.remainderUnsigned(value.getAndIncrement(), partitions);
    }
}
