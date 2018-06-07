package ru.kontur.vostok.hercules.meta.stream;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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

    @JsonIgnore
    public int[] partitionsForLogicalSharding(int k, int n) {
        if (partitions == n) {
            return new int[]{k};
        } else if (partitions < n) {
            if (k < partitions) {
                return new int[]{k};
            } else {
                return new int[]{};
            }
        } else {
            int[] res = new int[(partitions - k - 1)/ n + 1];
            for (int i = 0; i < res.length; ++i ) {
                res[i] = k + i * n;
            }
            return res;
        }
    }

    public long getTtl() {
        return ttl;
    }
    public void setTtl(long ttl) {
        this.ttl = ttl;
    }
}
