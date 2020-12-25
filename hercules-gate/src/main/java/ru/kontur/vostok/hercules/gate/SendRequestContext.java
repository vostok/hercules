package ru.kontur.vostok.hercules.gate;

import ru.kontur.vostok.hercules.meta.stream.Stream;
import ru.kontur.vostok.hercules.partitioner.ShardingKey;
import ru.kontur.vostok.hercules.protocol.TinyString;

import java.util.Set;

/**
 * @author Gregory Koshelev
 */
public class SendRequestContext {
    private final long requestTimestampMs;
    private final boolean async;
    private final Stream stream;
    private final Set<TinyString> tags;
    private final ShardingKey shardingKey;
    private final ContentValidator validator;

    public SendRequestContext(
            long requestTimestampMs,
            boolean async,
            Stream stream,
            Set<TinyString> tags,
            ShardingKey shardingKey,
            ContentValidator validator) {
        this.requestTimestampMs = requestTimestampMs;
        this.async = async;
        this.stream = stream;
        this.tags = tags;
        this.shardingKey = shardingKey;
        this.validator = validator;
    }

    public long requestTimestampMs() {
        return requestTimestampMs;
    }

    public boolean isAsync() {
        return async;
    }

    public String topic() {
        return stream.getName();
    }

    public Set<TinyString> tags() {
        return tags;
    }

    public int partitions() {
        return stream.getPartitions();
    }

    public ShardingKey shardingKey() {
        return shardingKey;
    }

    public ContentValidator validator() {
        return validator;
    }
}
