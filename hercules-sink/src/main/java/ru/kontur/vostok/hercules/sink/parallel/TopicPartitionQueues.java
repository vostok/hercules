package ru.kontur.vostok.hercules.sink.parallel;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;

import java.util.List;
import java.util.Set;

/**
 * Interface of queues of ConsumerRecord for each assigned TopicPartition.
 *
 * @author Innokentiy Krivonosov
 */
public interface TopicPartitionQueues {
    Set<TopicPartition> getPartitions();

    void addAll(TopicPartition partition, List<ConsumerRecord<byte[], byte[]>> records);

    ConsumerRecord<byte[], byte[]> poll(TopicPartition topicPartition);

    void removeQueue(TopicPartition topicPartition);

    Integer getQueuesSize(TopicPartition topicPartition);

    long getTotalByteSize();

    long getTotalCount();
}
