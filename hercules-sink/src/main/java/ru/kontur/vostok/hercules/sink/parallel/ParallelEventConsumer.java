package ru.kontur.vostok.hercules.sink.parallel;

import org.apache.kafka.clients.consumer.CommitFailedException;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.kafka.util.consumer.Subscription;
import ru.kontur.vostok.hercules.sink.Processor;
import ru.kontur.vostok.hercules.sink.SinkProps;
import ru.kontur.vostok.hercules.sink.metrics.KafkaEventSupplierStat;
import ru.kontur.vostok.hercules.sink.metrics.SinkMetrics;
import ru.kontur.vostok.hercules.util.concurrent.ThreadFactories;
import ru.kontur.vostok.hercules.util.lifecycle.Lifecycle;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.time.TimeSource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Consume event to {@code TopicPartitionQueues} for parallel processing
 * and commit offsets from other threads
 *
 * @author Innokentiy Krivonosov
 */
public class ParallelEventConsumer implements Lifecycle, ConsumerRebalanceListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParallelEventConsumer.class);

    private volatile boolean running = true;

    private final Processor processor;
    private final ConcurrentTopicPartitionQueues topicPartitionQueues;

    /**
     * Queue of commit offsets from other threads
     */
    private final BlockingQueue<Map<TopicPartition, OffsetAndMetadata>> offsetsToCommitQueue;

    private final ConsumerRebalanceListener strategyRebalanceListener;

    private final PartitionPauser partitionPauser = new PartitionPauser();

    private final ExecutorService executor = Executors.newSingleThreadExecutor(
            ThreadFactories.newNamedThreadFactory("consumer", false)
    );

    private final int batchSize;
    private final int maxEventsCount;
    private final long maxEventsByteSize;

    private final long pollTimeoutMs;
    private final long pollingDelayMs;
    private final long availabilityTimeoutMs;

    private final Pattern pattern;
    private final Consumer<byte[], byte[]> consumer;

    private final SinkMetrics metrics;
    private final KafkaEventSupplierStat stat;

    ParallelEventConsumer(
            Properties properties,
            Subscription subscription,
            Consumer<byte[], byte[]> consumer,
            ConcurrentTopicPartitionQueues topicPartitionQueues,
            BlockingQueue<Map<TopicPartition, OffsetAndMetadata>> offsetsToCommitQueue,
            Processor processor,
            ConsumerRebalanceListener strategyRebalanceListener,
            int batchSize,
            long batchByteSize,
            SinkMetrics metrics
    ) {
        this.processor = processor;
        this.topicPartitionQueues = topicPartitionQueues;
        this.offsetsToCommitQueue = offsetsToCommitQueue;
        this.strategyRebalanceListener = strategyRebalanceListener;

        this.pollTimeoutMs = PropertiesUtil.get(SinkProps.POLL_TIMEOUT_MS, properties).get();
        this.pollingDelayMs = PropertiesUtil.get(Props.POLLING_DELAY_MS, properties).get();
        this.availabilityTimeoutMs = PropertiesUtil.get(SinkProps.AVAILABILITY_TIMEOUT_MS, properties).get();

        this.batchSize = batchSize;
        this.maxEventsCount = Math.max(batchSize, PropertiesUtil.get(Props.MAX_EVENTS_COUNT, properties).get());
        this.maxEventsByteSize = Math.max(batchByteSize, PropertiesUtil.get(Props.MAX_EVENTS_BYTE_SIZE, properties).get());

        this.pattern = subscription.toPattern();
        this.consumer = consumer;

        this.metrics = metrics;
        this.stat = new KafkaEventSupplierStat(TimeSource.SYSTEM, () -> isRunning() ? this.consumer.assignment() : Collections.emptySet());
    }

    @Override
    public void start() {
        executor.execute(this::run);
    }

    void run() {
        while (isRunning()) {
            if (processor.isAvailable()) {
                try {
                    subscribe();

                    while (isRunning() && processor.isAvailable()) {
                        collectQueues();

                        commitOffsets(topicPartitionQueues.getPartitions());
                    }
                } catch (WakeupException ex) {
                    /*
                     * WakeupException is used to terminate consumer operations
                     */
                    return;
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (Exception ex) {
                    LOGGER.error("Unspecified exception has been acquired", ex);
                } finally {
                    unsubscribe();
                }
            }

            processor.awaitAvailability(availabilityTimeoutMs);
        }

        try {
            commitOffsets(topicPartitionQueues.getPartitions());
        } catch (Exception ex) {
            LOGGER.error("Unspecified exception on stopping", ex);
        }

        try {
            consumer.close();
        } catch (Exception ex) {
            /* ignore */
        }
    }

    /**
     * Poll consumer records and add to queues
     * Also waiting if the queues are full or the received items are less than the {@code batchSize}
     *
     * @throws InterruptedException if thread was interrupted
     */
    private void collectQueues() throws InterruptedException {
        stat.reset();
        partitionPauser.resumeIfAllowed();

        ConsumerRecords<byte[], byte[]> pollResult = poll();
        addToQueues(pollResult);
        topicPartitionQueues.signalNotEmpty();

        partitionPauser.pauseIfEnoughEvents(pollResult);
        logAndUpdateMetric(pollResult);
        await(pollResult);
    }

    private void logAndUpdateMetric(ConsumerRecords<byte[], byte[]> pollResult) {
        long totalEvents = topicPartitionQueues.getTotalCount();

        if (totalEvents != 0 && LOGGER.isDebugEnabled()) {
            LOGGER.debug("Poll result: " + pollResult.count() + ", total count: " + totalEvents +
                    " (" + topicPartitionQueues.getTotalByteSize() + " bytes), on pause: " +
                    consumer.paused().size() + " " + consumer.paused());
        }

        stat.setTotalEventCount(totalEvents);
        metrics.update(stat);
    }

    private void await(ConsumerRecords<byte[], byte[]> pollResult) throws InterruptedException {
        if (isFull()) {
            do {
                topicPartitionQueues.awaitNotFull(pollingDelayMs, TimeUnit.MILLISECONDS);
            } while (isRunning() && isFull());
        } else if (pollResult.count() < batchSize) {
            TimeUnit.MILLISECONDS.sleep(pollingDelayMs);
        }
    }

    private boolean isFull() {
        return topicPartitionQueues.getTotalCount() >= maxEventsCount ||
                topicPartitionQueues.getTotalByteSize() >= maxEventsByteSize;
    }

    /**
     * Add ConsumerRecords to queues
     *
     * @param pollResult poll records
     */
    private void addToQueues(ConsumerRecords<byte[], byte[]> pollResult) {
        Set<TopicPartition> partitions = pollResult.partitions();

        for (TopicPartition partition : partitions) {
            List<ConsumerRecord<byte[], byte[]>> records = pollResult.records(partition);

            topicPartitionQueues.addAll(partition, records);
            stat.incrementTotalEventsPerPartition(partition, records.size());
            stat.incrementBatchSizePerPartition(partition, records.stream().mapToInt(ConsumerRecord::serializedValueSize).sum());
        }
    }

    class PartitionPauser {
        /**
         * Suspend fetching from the partitions in the queue for which events are greater than the {@code batchSize}
         *
         * @param consumerRecords polled records
         */
        private void pauseIfEnoughEvents(ConsumerRecords<byte[], byte[]> consumerRecords) {
            List<TopicPartition> newPartitionsToPause = new ArrayList<>();

            for (TopicPartition partition : consumerRecords.partitions()) {
                Integer queueSize = topicPartitionQueues.getQueuesSize(partition);
                if (queueSize != null && queueSize >= batchSize) {
                    newPartitionsToPause.add(partition);
                }
            }

            if (isRunning() && !newPartitionsToPause.isEmpty()) {
                consumer.pause(newPartitionsToPause);
            }
        }

        /**
         * Resume fetching from the partitions in the queue for which events are lower than the {@code batchSize}
         */
        private void resumeIfAllowed() {
            Set<TopicPartition> partitionsToResume = new HashSet<>();
            for (TopicPartition partition : consumer.paused()) {
                Integer queueSize = topicPartitionQueues.getQueuesSize(partition);
                if (queueSize == null || queueSize < batchSize) {
                    partitionsToResume.add(partition);
                }
            }

            if (isRunning() && !partitionsToResume.isEmpty()) {
                consumer.resume(partitionsToResume);
            }
        }
    }

    /**
     * Commit offsets from {@code offsetsToCommitQueue}
     *
     * @param activeTopicPartition active partition
     */
    private void commitOffsets(Collection<TopicPartition> activeTopicPartition) {
        while (!offsetsToCommitQueue.isEmpty()) {
            Map<TopicPartition, OffsetAndMetadata> offsetsToCommit = offsetsToCommitQueue.peek();
            Map<TopicPartition, OffsetAndMetadata> allowedOffsetsToCommit = offsetsToCommit.entrySet().stream()
                    .filter(entry -> activeTopicPartition.contains(entry.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            if (!allowedOffsetsToCommit.isEmpty()) {
                try {
                    consumer.commitSync(allowedOffsetsToCommit);
                    offsetsToCommitQueue.remove();
                } catch (CommitFailedException ex) {
                    LOGGER.warn("Commit failed due to rebalancing: " + allowedOffsetsToCommit.keySet() +
                            ", skip not active partition: " + offsetsToCommit.keySet().stream()
                            .filter(it -> !allowedOffsetsToCommit.containsKey(it))
                            .collect(Collectors.toList()));
                    return;
                }
            } else {
                LOGGER.warn("Skip not active partition: " + offsetsToCommit.keySet());
                offsetsToCommitQueue.remove();
            }
        }
    }

    /**
     * Check KafkaEventSupplier running status.
     *
     * @return {@code true} if KafkaDataSupplier is running and {@code false} if KafkaDataSupplier is stopping
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Poll Events from Kafka. Should be called when KafkaDataSupplier subscribed.
     *
     * @return polled Events
     * @throws WakeupException if poll terminated due to shutdown
     */
    private ConsumerRecords<byte[], byte[]> poll() {
        stat.markPollStart();
        ConsumerRecords<byte[], byte[]> consumerRecords = consumer.poll(Duration.ofMillis(pollTimeoutMs));
        stat.markPollEnd(consumerRecords.count());
        return consumerRecords;
    }

    /**
     * Subscribe Sink. Should be called before polling
     */
    protected final void subscribe() {
        consumer.subscribe(pattern, this);
    }

    /**
     * Unsubscribe Sink. Should be called if Sink cannot process Events.
     */
    protected final void unsubscribe() {
        LOGGER.debug("Sink unsubscribe if any");
        try {
            consumer.unsubscribe();
        } catch (Exception ex) {
            /* ignore */
        }
    }

    /**
     * Queues for revoked partitions are removed
     * and a strategy is called to wait for the completion of already sended events from these partitions
     *
     * @param partitions The list of partitions that were assigned to the consumer and now need to be revoked (may not
     *                   include all currently assigned partitions, i.e. there may still be some partitions left)
     */
    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        Set<TopicPartition> previousActiveTopicPartitions = new HashSet<>(topicPartitionQueues.getPartitions());
        partitions.forEach(topicPartitionQueues::removeQueue);

        strategyRebalanceListener.onPartitionsRevoked(partitions);

        commitOffsets(previousActiveTopicPartitions);
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        //do nothing
    }

    @Override
    public boolean stop(long timeout, TimeUnit timeUnit) {
        running = false;

        try {
            consumer.wakeup();
        } catch (Exception ex) {
            /* ignore */
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(timeout, timeUnit)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(timeout, timeUnit)) {
                    LOGGER.warn("Thread pool did not terminate");
                }
                return false;
            }
            return true;
        } catch (InterruptedException e) {
            LOGGER.error("KafkaEventSupplier shutdown execute was terminated by InterruptedException", e);
            return false;
        }
    }

    static class Props {
        static final Parameter<Long> POLLING_DELAY_MS =
                Parameter.longParameter("pollingDelayMs")
                        .withDefault(100L)
                        .build();

        static final Parameter<Integer> MAX_EVENTS_COUNT =
                Parameter.integerParameter("maxEventsCount")
                        .withDefault(10000)
                        .build();

        static final Parameter<Long> MAX_EVENTS_BYTE_SIZE =
                Parameter.longParameter("maxEventsByteSize")
                        .withDefault(100_000_000L)
                        .build();
    }
}
