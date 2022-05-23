package ru.kontur.vostok.hercules.partitioner.hash;

import ru.kontur.vostok.hercules.partitioner.ShardingKey;
import ru.kontur.vostok.hercules.protocol.Event;

/**
 * @author Gregory Koshelev
 */
public interface Hasher {
    int hash(Event event, ShardingKey shardingKey);
}
