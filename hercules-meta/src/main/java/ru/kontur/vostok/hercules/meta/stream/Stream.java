package ru.kontur.vostok.hercules.meta.stream;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * @author Gregory Koshelev
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = BaseStream.class, name = "base"),
        @JsonSubTypes.Type(value = DerivedStream.class, name = "derived")
})
public abstract class Stream {
    private String name;
    private int partitions;
    private String[] shardingKey;
    private long ttl;

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public int getPartitions() {
        return partitions;
    }
    public void setPartitions(int partitions) {
        this.partitions = partitions;
    }

    public String[] getShardingKey() {
        return shardingKey;
    }
    public void setShardingKey(String[] shardingKey) {
        this.shardingKey = shardingKey;
    }

    public long getTtl() {
        return ttl;
    }
    public void setTtl(long ttl) {
        this.ttl = ttl;
    }
}
