package ru.kontur.vostok.hercules.partitioner;

import ru.kontur.vostok.hercules.protocol.Event;

/**
 * Partitioner provides partition number for an event.
 *
 * @author Gregory Koshelev
 */
public interface Partitioner {
    /**
     * Returns partition number for an event.
     * <p>
     * Implementation may use {@link ShardingKey} to determine partition number.
     * The number must be in the range {@code 0 <= value < partitions}.
     *
     * @param event       an event
     * @param shardingKey the sharding key
     * @param partitions  the partition count
     * @return partition number
     */
    int partition(Event event, ShardingKey shardingKey, int partitions);
}
