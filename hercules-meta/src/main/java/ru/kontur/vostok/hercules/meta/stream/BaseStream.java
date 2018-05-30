package ru.kontur.vostok.hercules.meta.stream;

/**
 * @author Gregory Koshelev
 */
public class BaseStream {
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
