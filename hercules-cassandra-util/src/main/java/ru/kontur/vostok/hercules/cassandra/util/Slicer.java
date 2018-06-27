package ru.kontur.vostok.hercules.cassandra.util;

import ru.kontur.vostok.hercules.partitioner.Partitioner;
import ru.kontur.vostok.hercules.protocol.Event;

/**
 * @author Gregory Koshelev
 */
public class Slicer {
    private final Partitioner partitioner;
    private final Partitioner defaultPartitioner;
    private final String[] shardingKey;
    private int slices;

    public Slicer(Partitioner partitioner, Partitioner defaultPartitioner, String[] shardingKey, int slices) {
        this.partitioner = partitioner;
        this.defaultPartitioner = defaultPartitioner;
        this.shardingKey = shardingKey;
        this.slices = slices;
    }

    public int slice(Event event) {
        return (shardingKey != null && shardingKey.length > 0) ? partitioner.partition(event, shardingKey, slices) : defaultPartitioner.partition(event, shardingKey, slices);
    }
}
