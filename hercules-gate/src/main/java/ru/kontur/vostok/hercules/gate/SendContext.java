package ru.kontur.vostok.hercules.gate;

import ru.kontur.vostok.hercules.uuid.Marker;

import java.util.Set;

/**
 * @author Gregory Koshelev
 */
public class SendContext {
    final boolean async;
    final Marker marker;
    final String topic;
    final Set<String> tags;
    final int partitions;
    final String[] shardingKey;
    final ContentValidator validator;

    public SendContext(boolean async, Marker marker, String topic, Set<String> tags, int partitions, String[] shardingKey, ContentValidator validator) {
        this.async = async;
        this.marker = marker;
        this.topic = topic;
        this.tags = tags;
        this.partitions = partitions;
        this.shardingKey = shardingKey;
        this.validator = validator;
    }
}
