package ru.kontur.vostok.hercules.sink.parallel;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Queues wrapper of ConsumerRecord for each assigned TopicPartition.
 * Has methods for waiting for non-empty and non-full states for all queues.
 *
 * @author Innokentiy Krivonosov
 */
public class TopicPartitionQueuesImpl implements ConcurrentTopicPartitionQueues {
    /**
     * Map of TopicPartition to ConsumerRecord's queue
     */
    private final ConcurrentMap<TopicPartition, BlockingQueue<ConsumerRecord<byte[], byte[]>>> queues;

    private final ReentrantLock lock;
    private final Condition notEmpty;
    private final Condition notFull;
    private final AtomicLong totalByteSize = new AtomicLong();
    private final AtomicLong totalEventCount = new AtomicLong();

    public TopicPartitionQueuesImpl() {
        this.queues = new ConcurrentHashMap<>();
        this.lock = new ReentrantLock();
        this.notEmpty = this.lock.newCondition();
        this.notFull = this.lock.newCondition();
    }

    @Override
    public Set<TopicPartition> getPartitions() {
        return queues.keySet();
    }

    @Override
    public void addAll(TopicPartition partition, List<ConsumerRecord<byte[], byte[]>> records) {
        BlockingQueue<ConsumerRecord<byte[], byte[]>> queue = queues.computeIfAbsent(partition, (k) -> new LinkedBlockingQueue<>());
        queue.addAll(records);
        totalEventCount.addAndGet(records.size());
        totalByteSize.addAndGet(records.stream().mapToInt(ConsumerRecord::serializedValueSize).sum());
    }

    @Override
    public @Nullable ConsumerRecord<byte[], byte[]> poll(TopicPartition topicPartition) {
        BlockingQueue<ConsumerRecord<byte[], byte[]>> consumerRecords = queues.get(topicPartition);
        if (consumerRecords != null) {
            return poll(consumerRecords);
        }

        return null;
    }

    @Override
    public void removeQueue(TopicPartition topicPartition) {
        BlockingQueue<ConsumerRecord<byte[], byte[]>> queue = queues.remove(topicPartition);
        if (queue != null) {
            while (!queue.isEmpty()) {
                poll(queue);
            }
        }
    }

    @Nullable
    private ConsumerRecord<byte[], byte[]> poll(BlockingQueue<ConsumerRecord<byte[], byte[]>> consumerRecords) {
        ConsumerRecord<byte[], byte[]> consumerRecord = consumerRecords.poll();
        if (consumerRecord != null) {
            totalByteSize.addAndGet(-consumerRecord.serializedValueSize());
            totalEventCount.decrementAndGet();
            return consumerRecord;
        }
        return null;
    }

    @Override
    public @Nullable Integer getQueuesSize(TopicPartition topicPartition) {
        BlockingQueue<ConsumerRecord<byte[], byte[]>> consumerRecords = queues.get(topicPartition);
        if (consumerRecords != null) {
            return consumerRecords.size();
        } else {
            return null;
        }
    }

    @Override
    public long getTotalByteSize() {
        return totalByteSize.get();
    }

    @Override
    public long getTotalCount() {
        return totalEventCount.get();
    }

    @Override
    public void awaitNotFull(long timeout, TimeUnit unit) throws InterruptedException {
        ReentrantLock lock = this.lock;
        lock.lock();

        try {
            this.notFull.await(timeout, unit);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void signalNotFull() {
        ReentrantLock lock = this.lock;
        lock.lock();

        try {
            this.notFull.signal();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void awaitNotEmpty(long timeout, TimeUnit unit) throws InterruptedException {
        ReentrantLock lock = this.lock;
        lock.lock();

        try {
            this.notEmpty.await(timeout, unit);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void signalNotEmpty() {
        ReentrantLock lock = this.lock;
        lock.lock();

        try {
            this.notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }
}
