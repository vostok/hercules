package ru.kontur.vostok.hercules.partitioner;

import ru.kontur.vostok.hercules.protocol.Event;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Partitioner returns random partition number.
 *
 * @author Gregory Koshelev
 */
public class RandomPartitioner implements Partitioner {
    @Override
    public int partition(Event event, ShardingKey shardingKey, int partitions) {
        return Integer.remainderUnsigned(ThreadLocalRandom.current().nextInt(), partitions);
    }
}
