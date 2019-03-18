package ru.kontur.vostok.hercules.gate;

import ru.kontur.vostok.hercules.partitioner.ShardingKey;

import java.util.Set;

/**
 * @author Gregory Koshelev
 */
public class SendContext {
    private final boolean async;
    private final String topic;
    private final Set<String> tags;
    private final int partitions;
    private final ShardingKey shardingKey;
    private final ContentValidator validator;

    public SendContext(
            boolean async,
            String topic,
            Set<String> tags,
            int partitions,
            ShardingKey shardingKey,
            ContentValidator validator
    ) {
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

    public Set<String> getTags() {
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
