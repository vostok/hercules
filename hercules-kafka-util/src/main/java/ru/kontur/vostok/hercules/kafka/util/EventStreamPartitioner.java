package ru.kontur.vostok.hercules.kafka.util;

import org.apache.kafka.streams.processor.StreamPartitioner;
import ru.kontur.vostok.hercules.partitioner.Partitioner;
import ru.kontur.vostok.hercules.protocol.Event;

/**
 * @author Gregory Koshelev
 */
public class EventStreamPartitioner implements StreamPartitioner<Void, Event> {
    private final Partitioner partitioner;
    private final String[] shardingKey;
    private final int partitions;

    public EventStreamPartitioner(Partitioner partitioner, String[] shardingKey, int partitions) {
        this.partitioner = partitioner;
        this.shardingKey = shardingKey;
        this.partitions = partitions;
    }

    @Override
    public Integer partition(Void key, Event value, int numPartitions) {
        return (shardingKey != null && shardingKey.length > 0) ? partitioner.partition(value, shardingKey, partitions) : null;
    }
}
