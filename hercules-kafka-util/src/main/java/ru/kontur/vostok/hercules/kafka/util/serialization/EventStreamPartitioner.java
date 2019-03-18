package ru.kontur.vostok.hercules.kafka.util.serialization;

import org.apache.kafka.streams.processor.StreamPartitioner;
import ru.kontur.vostok.hercules.partitioner.Partitioner;
import ru.kontur.vostok.hercules.partitioner.ShardingKey;
import ru.kontur.vostok.hercules.protocol.Event;

import java.util.UUID;

/**
 * @author Gregory Koshelev
 */
public class EventStreamPartitioner implements StreamPartitioner<UUID, Event> {
    private final Partitioner partitioner;
    private final ShardingKey shardingKey;
    private final int partitions;

    public EventStreamPartitioner(Partitioner partitioner, ShardingKey shardingKey, int partitions) {
        this.partitioner = partitioner;
        this.shardingKey = shardingKey;
        this.partitions = partitions;
    }

    @Override
    public Integer partition(String topic, UUID key, Event value, int numPartitions) {
        return (shardingKey != null && !shardingKey.isEmpty()) ? partitioner.partition(value, shardingKey, partitions) : null;
    }
}
