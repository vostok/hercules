package ru.kontur.vostok.hercules.partitioner;

import ru.kontur.vostok.hercules.protocol.Event;

/**
 * @author Gregory Koshelev
 */
public interface Partitioner {
    int partition(Event event, ShardingKey shardingKey, int partitions);
}
