package ru.kontur.vostok.hercules.gate;

import ru.kontur.vostok.hercules.uuid.Marker;

import java.util.Set;

/**
 * @author Gregory Koshelev
 */
public class SendContext {
    private final boolean async;
    private final Marker marker;
    private final String topic;
    private final Set<String> tags;
    private final int partitions;
    private final String[] shardingKey;
    private final ContentValidator validator;

    public SendContext(
            boolean async,
            Marker marker,
            String topic,
            Set<String> tags,
            int partitions,
            String[] shardingKey,
            ContentValidator validator
    ) {
        this.async = async;
        this.marker = marker;
        this.topic = topic;
        this.tags = tags;
        this.partitions = partitions;
        this.shardingKey = shardingKey;
        this.validator = validator;
    }

    public boolean isAsync() {
        return async;
    }

    public Marker getMarker() {
        return marker;
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

    public String[] getShardingKey() {
        return shardingKey;
    }

    public ContentValidator getValidator() {
        return validator;
    }
}
