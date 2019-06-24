package ru.kontur.vostok.hercules.stream.api;

import org.apache.kafka.common.TopicPartition;
import ru.kontur.vostok.hercules.meta.stream.Stream;
import ru.kontur.vostok.hercules.partitioner.LogicalPartitioner;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Gregory Koshelev
 */
public final class StreamUtil {
    public static List<TopicPartition> getTopicPartitions(Stream stream, int shardIndex, int shardCount) {
        int[] partitions = LogicalPartitioner.getPartitionsForLogicalSharding(stream, shardIndex, shardCount);
        List<TopicPartition> topicPartitions = new ArrayList<>(partitions.length);
        for (int partition : partitions) {
            topicPartitions.add(new TopicPartition(stream.getName(), partition));
        }
        return topicPartitions;
    }

    private StreamUtil() {
        /* static class */
    }
}
