package ru.kontur.vostok.hercules.gate;

import ru.kontur.vostok.hercules.partitioner.ShardingKey;
import ru.kontur.vostok.hercules.protocol.TinyString;

import java.util.Set;

/**
 * @author Gregory Koshelev
 */
public class SendRequestContext {
    private final boolean async;
    private final String topic;
    private final Set<TinyString> tags;
    private final int partitions;
    private final ShardingKey shardingKey;
    private final ContentValidator validator;

    public SendRequestContext(
            boolean async,
            String topic,
            Set<TinyString> tags,
            int partitions,
            ShardingKey shardingKey,
            ContentValidator validator) {
        this.async = async;
        this.topic = topic;
        this.tags = tags;
        this.partitions = partitions;
        this.shardingKey = shardingKey;
        this.validator = validator;
    }

    public boolean isAsync() {
        return async;
    }

    public String getTopic() {
        return topic;
    }

    public Set<TinyString> getTags() {
        return tags;
    }

    public int getPartitions() {
        return partitions;
    }

    public ShardingKey getShardingKey() {
        return shardingKey;
    }

    public ContentValidator getValidator() {
        return validator;
    }
}
