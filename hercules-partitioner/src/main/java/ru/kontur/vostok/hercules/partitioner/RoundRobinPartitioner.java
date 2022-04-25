package ru.kontur.vostok.hercules.partitioner;

import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.util.number.IntegerUtil;
import ru.kontur.vostok.hercules.util.random.RandomUtil;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Partitioner returns partition number using round robin algorithm.
 * <p>
 * Iteration works across multiple threads.
 *
 * @author Gregory Koshelev
 */
public class RoundRobinPartitioner implements Partitioner {
    private final AtomicInteger value = new AtomicInteger(RandomUtil.generateIntSeed());

    @Override
    public int partition(Event event, ShardingKey shardingKey, int partitions) {
        return IntegerUtil.toPositive(value.getAndIncrement()) % partitions;
    }
}
