package ru.kontur.vostok.hercules.sink.parallel;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.time.TimeSource;
import ru.kontur.vostok.hercules.util.time.Timer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Create EventsBatch strategy
 * Two stage algorithm:
 * create batch immediately - {@link #fullPartition(Predicate)}
 * only after createBatchTimeoutMs, for prevent sending many small batches - {@link #notFullPartition(Predicate)}
 *
 * @author Innokentiy Krivonosov
 */
public class CreateEventsBatchStrategyImpl<T> implements CreateEventsBatchStrategy<T> {
    private final TopicPartitionQueues topicPartitionQueues;
    private final Timer createBatchTimeoutTimer;

    private final int batchSize;
    private final int maxPartitionsInBatch;
    private final long batchByteSize;

    private List<TopicPartition> topicPartitions;

    private int fullCircularIterator = 0;
    private boolean useFullPartition = false;

    public CreateEventsBatchStrategyImpl(
            Properties strategyProperties,
            TopicPartitionQueues topicPartitionQueues,
            int sinkBatchSize,
            long batchByteSize) {
        this.topicPartitionQueues = topicPartitionQueues;
        this.batchByteSize = batchByteSize;

        if (!PropertiesUtil.get(Props.BATCH_SIZE, strategyProperties).isEmpty()) {
            this.batchSize = PropertiesUtil.get(Props.BATCH_SIZE, strategyProperties).get();
        } else {
            this.batchSize = sinkBatchSize;
        }

        this.maxPartitionsInBatch = PropertiesUtil.get(Props.MAX_PARTITIONS_IN_BATCH, strategyProperties).get();

        long createBatchTimeoutMs = PropertiesUtil.get(Props.CREATE_BATCH_TIMEOUT_MS, strategyProperties).get();

        this.createBatchTimeoutTimer = TimeSource.SYSTEM.timer(createBatchTimeoutMs);
    }

    /**
     * Changes stages {@link #useFullPartition} on each call
     *
     * @param isAllowedPartition allowed partition for process
     * @return new batches for processing
     */
    @Override
    public List<EventsBatch<T>> create(Predicate<TopicPartition> isAllowedPartition) {
        if (updateTopicPartitions()) {
            return Collections.emptyList();
        }

        useFullPartition = !useFullPartition;

        if (useFullPartition) {
            List<EventsBatch<T>> eventsBatches = fullPartition(isAllowedPartition);
            if (eventsBatches.isEmpty() && createBatchTimeoutTimer.isExpired()) {
                return notFullPartition(isAllowedPartition);
            } else {
                return eventsBatches;
            }
        } else {
            if (createBatchTimeoutTimer.isExpired()) {
                List<EventsBatch<T>> eventsBatches = notFullPartition(isAllowedPartition);
                if (!eventsBatches.isEmpty()) {
                    return eventsBatches;
                }
            }

            return fullPartition(isAllowedPartition);
        }
    }

    /**
     * Strategy create batches from not full queues
     * Use <a href="https://en.wikipedia.org/wiki/First-fit-decreasing_bin_packing">First Fit Decreasing</a> algorithm
     *
     * @param isAllowedPartition allowed partition for process
     * @return new batches for processing
     */
    private List<EventsBatch<T>> notFullPartition(Predicate<TopicPartition> isAllowedPartition) {
        createBatchTimeoutTimer.reset();

        Map<TopicPartition, Integer> allowedBatchPartitions = new HashMap<>();

        for (TopicPartition topicPartition : topicPartitions) {
            if (isAllowedPartition.test(topicPartition)) {
                Integer queueSize = topicPartitionQueues.getQueuesSize(topicPartition);
                if (queueSize != null && queueSize > 0 && queueSize < batchSize) {
                    allowedBatchPartitions.put(topicPartition, queueSize);
                }
            }
        }

        if (!allowedBatchPartitions.isEmpty()) {
            List<EventsBatch.EventsBatchBuilder<T>> batches = new ArrayList<>();

            allowedBatchPartitions.entrySet().stream()
                    .sorted(Collections.reverseOrder(Comparator.comparingInt(Map.Entry::getValue)))
                    .forEach(entry -> {
                        TopicPartition topicPartition = entry.getKey();
                        int queueSize = entry.getValue();
                        findBatchForQueueAndAdd(batches, topicPartition, queueSize);
                    });

            return batches.stream()
                    .map(EventsBatch.EventsBatchBuilder::build)
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    /**
     * Find first fit batch for events from partition or add to new batch
     *
     * @param batches        already created bathces
     * @param topicPartition add partition
     * @param queueSize      event in queue
     */
    private void findBatchForQueueAndAdd(
            List<EventsBatch.EventsBatchBuilder<T>> batches,
            TopicPartition topicPartition,
            int queueSize
    ) {
        for (EventsBatch.EventsBatchBuilder<T> batch : batches) {
            if (batch.offsetsToCommit.size() < maxPartitionsInBatch &&
                    batchSize - batch.rawEventsCount >= queueSize
            ) {
                addEventsToBatch(batch, topicPartition, queueSize);
                return;
            }
        }

        EventsBatch.EventsBatchBuilder<T> newEventsBatch = new EventsBatch.EventsBatchBuilder<>();
        batches.add(newEventsBatch);
        addEventsToBatch(newEventsBatch, topicPartition, queueSize);
    }

    /**
     * Create batch immediately
     *
     * @param isAllowedPartition allowed partition for process
     * @return new batches for processing
     */
    private List<EventsBatch<T>> fullPartition(Predicate<TopicPartition> isAllowedPartition) {
        for (int i = 0; i < topicPartitions.size(); i++) {
            TopicPartition topicPartition = topicPartitions.get(fullCircularIterator++ % topicPartitions.size());

            if (isAllowedPartition.test(topicPartition)) {
                Integer queueSize = topicPartitionQueues.getQueuesSize(topicPartition);

                if (queueSize != null && queueSize >= batchSize) {
                    EventsBatch.EventsBatchBuilder<T> batch = new EventsBatch.EventsBatchBuilder<>();
                    addEventsToBatch(batch, topicPartition, queueSize);
                    return Collections.singletonList(batch.build());
                }
            }
        }

        return Collections.emptyList();
    }

    /**
     * Add events and offsets to batch
     *
     * @param batch          EventsBatch
     * @param topicPartition processes topicPartition
     * @param queueSize      topicPartition queue size
     */
    private void addEventsToBatch(
            EventsBatch.EventsBatchBuilder<T> batch,
            TopicPartition topicPartition,
            int queueSize
    ) {
        List<byte[]> events = new ArrayList<>(queueSize);
        long offset = -1;
        long newRawEventsByteSize = 0;

        while (batch.rawEventsCount + events.size() < batchSize &&
                batch.rawEventsByteSize + newRawEventsByteSize < batchByteSize
        ) {
            ConsumerRecord<byte[], byte[]> consumerRecord = topicPartitionQueues.poll(topicPartition);
            if (consumerRecord != null) {
                events.add(consumerRecord.value());
                offset = consumerRecord.offset() + 1;
                newRawEventsByteSize += consumerRecord.value().length;
            } else {
                break;
            }
        }

        if (!events.isEmpty()) {
            batch.rawEvents.put(topicPartition, events);
            batch.offsetsToCommit.put(topicPartition, new OffsetAndMetadata(offset));
            batch.rawEventsCount += events.size();
            batch.rawEventsByteSize += newRawEventsByteSize;
        }
    }

    /**
     * Synchronize topicPartitions with topicPartitionQueues
     *
     * @return true if successfully synchronize and has not empty partitions
     */
    private boolean updateTopicPartitions() {
        Set<TopicPartition> topicPartitions = this.topicPartitionQueues.getPartitions();
        if (this.topicPartitions == null ||
                topicPartitions.size() != this.topicPartitions.size() ||
                this.topicPartitions.stream().noneMatch(topicPartitions::contains)
        ) {
            this.topicPartitions = new ArrayList<>(topicPartitions);
            return this.topicPartitions.isEmpty();
        }

        return false;
    }

    static class Props {
        static final Parameter<Integer> BATCH_SIZE = Parameter.integerParameter("batchSize").build();

        static final Parameter<Integer> MAX_PARTITIONS_IN_BATCH =
                Parameter.integerParameter("maxPartitionsInBatch")
                        .withDefault(10)
                        .build();

        static final Parameter<Long> CREATE_BATCH_TIMEOUT_MS =
                Parameter.longParameter("createBatchTimeoutMs")
                        .withDefault(1_000L)
                        .build();
    }
}
