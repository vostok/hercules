package ru.kontur.vostok.hercules.stream.manager.kafka;

import ru.kontur.vostok.hercules.meta.stream.Stream;

import java.util.Objects;

/**
 * POJO abstraction describes topic in Kafka.
 *
 * @author Gregory Koshelev
 */
public final class Topic {
    private final String name;
    private final int partitions;
    private final long ttl;

    Topic(String name, int partitions, long ttl) {
        this.name = name;
        this.partitions = partitions;
        this.ttl = ttl;
    }

    public String name() {
        return name;
    }

    public int partitions() {
        return partitions;
    }

    public long ttl() {
        return ttl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Topic topic = (Topic) o;
        return partitions == topic.partitions &&
                ttl == topic.ttl &&
                Objects.equals(name, topic.name);
    }

    @Override
    public int hashCode() {
        return ((31 + name.hashCode()) * 31 + Integer.hashCode(partitions)) * 31 + Long.hashCode(ttl);
    }

    @Override
    public String toString() {
        return "Topic{name='" + name + "', partitions=" + partitions + ", ttl=" + ttl + "}";
    }

    public static Topic forStream(Stream stream) {
        return new Topic(stream.getName(), stream.getPartitions(), stream.getTtl());
    }
}
