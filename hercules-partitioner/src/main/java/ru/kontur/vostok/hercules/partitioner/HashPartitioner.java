package ru.kontur.vostok.hercules.partitioner;

import ru.kontur.vostok.hercules.partitioner.hash.Hasher;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.util.number.IntegerUtil;

/**
 * Partitioner returns partition value according to {@link Hasher} implementation.
 *
 * @author Gregory Koshelev
 */
public class HashPartitioner implements Partitioner {
    private final Hasher hasher;

    public HashPartitioner(Hasher hasher) {
        this.hasher = hasher;
    }

    @Override
    public int partition(Event event, ShardingKey shardingKey, int partitions) {
        int shardingHash = hasher.hash(event, shardingKey);
        return IntegerUtil.toPositive(shardingHash) % partitions;
    }
}
