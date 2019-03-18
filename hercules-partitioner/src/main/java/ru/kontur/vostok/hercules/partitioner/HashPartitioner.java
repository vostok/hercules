package ru.kontur.vostok.hercules.partitioner;

import ru.kontur.vostok.hercules.protocol.Event;

/**
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
        return (shardingHash & 0x7FFFFFFF) % partitions;
    }
}
