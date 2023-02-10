package ru.kontur.vostok.hercules.sink.parallel;

import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.sink.ProcessorResult;
import ru.kontur.vostok.hercules.sink.metrics.SinkMetrics;
import ru.kontur.vostok.hercules.sink.parallel.sender.PreparedData;
import ru.kontur.vostok.hercules.util.concurrent.ThreadFactories;
import ru.kontur.vostok.hercules.util.lifecycle.Lifecycle;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.time.TimeSource;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Parallel batch sending strategy.
 * Uses prepare and send executors to process batches.
 * Processing stages run cyclically in a method {@link #run()}
 *
 * @author Innokentiy Krivonosov
 */
public class ParallelSenderStrategy<T extends PreparedData> implements Lifecycle, ConsumerRebalanceListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParallelSenderStrategy.class);

    private volatile boolean running = true;
    volatile boolean partitionsRevokedStarted = false;

    private final int sendPoolSize;
    private final long maxAllBatchesByteSize;
    private final long waitBatchesPeriodMs;
    private final int fakeSenderDelay;

    private final ConcurrentTopicPartitionQueues topicPartitionQueues;
    private final BlockingQueue<Map<TopicPartition, OffsetAndMetadata>> offsetsToCommitQueue;

    private final CreateEventsBatchStrategy<T> createEventsBatchStrategy;

    private final ExecutorService strategyExecutor = Executors.newSingleThreadExecutor(
            ThreadFactories.newNamedThreadFactory("strategy", false)
    );

    private final ExecutorService processEventExecutor;
    private final ExecutorService sendEventExecutor;

    private final PrepareExecutor<T> prepareExecutor;
    private final SendExecutor<T> sendExecutor;

    private final Set<EventsBatch<T>> prepareEventBatches = new HashSet<>();
    private final Set<EventsBatch<T>> sendingEventBatches = new HashSet<>();

    private final SinkMetrics metrics;
    private final EventsBatchListener<T> eventsBatchListener;

    private boolean isAnyChanged = false;

    public ParallelSenderStrategy(
            Properties properties,
            CreateEventsBatchStrategy<T> createEventsBatchStrategy,
            PrepareExecutor<T> prepareExecutor,
            SendExecutor<T> sendExecutor,
            ConcurrentTopicPartitionQueues topicPartitionQueues,
            BlockingQueue<Map<TopicPartition, OffsetAndMetadata>> offsetsToCommitQueue,
            SinkMetrics sinkMetrics,
            EventsBatchListener<T> eventsBatchListener,
            long batchByteSize
    ) {
        this.createEventsBatchStrategy = createEventsBatchStrategy;
        this.prepareExecutor = prepareExecutor;
        this.sendExecutor = sendExecutor;
        this.topicPartitionQueues = topicPartitionQueues;
        this.offsetsToCommitQueue = offsetsToCommitQueue;
        this.metrics = sinkMetrics;
        this.eventsBatchListener = eventsBatchListener;

        this.waitBatchesPeriodMs = PropertiesUtil.get(Props.WAIT_BATCHES_PERIOD_MS, properties).get();
        this.maxAllBatchesByteSize = Math.max(batchByteSize, PropertiesUtil.get(Props.MAX_ALL_BATCHES_BYTE_SIZE, properties).get());
        this.fakeSenderDelay = PropertiesUtil.get(Props.FAKE_SENDER_DELAY, properties).get();

        int preparePoolSize = PropertiesUtil.get(Props.PREPARE_POOL_SIZE, properties).get();
        this.processEventExecutor = Executors.newFixedThreadPool(
                preparePoolSize, ThreadFactories.newNamedThreadFactory("prepare-events", false)
        );

        this.sendPoolSize = PropertiesUtil.get(Props.SEND_POOL_SIZE, properties).get();
        this.sendEventExecutor = Executors.newFixedThreadPool(
                sendPoolSize, ThreadFactories.newNamedThreadFactory("send-events", false)
        );
    }

    /**
     * Start sender.
     */
    @Override
    public void start() {
        strategyExecutor.execute(this::run);
    }

    /**
     * Main parallel sender logic.
     */
    private void run() {
        while (isRunning()) {
            try {
                isAnyChanged = false;

                createAndPrepareBatches();
                sendBatches();
                addToCommitAndRemoveProcessedBatches();

                checkOnlyAssignedPartitionsInSend();

                if (!isAnyChanged) {
                    topicPartitionQueues.awaitNotEmpty(waitBatchesPeriodMs, TimeUnit.MILLISECONDS);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                LOGGER.error("Unspecified exception has been acquired", ex);
            }
        }

        stopping();
    }

    /**
     * Creates new batches using the CreateEventsBatchStrategy and sends them for prepare
     */
    void createAndPrepareBatches() {
        if (prepareEventBatches.size() < sendPoolSize && getTotalByteSize() < maxAllBatchesByteSize) {
            List<EventsBatch<T>> batches = createEventsBatchStrategy.create(this::isAllowedToPrepare);

            if (!batches.isEmpty()) {
                topicPartitionQueues.signalNotFull();
                for (EventsBatch<T> batch : batches) {
                    prepareBatch(batch);
                }
            }
        }
    }

    /**
     * Send batch to prepare in processEventExecutor
     *
     * @param batch events batch
     */
    private void prepareBatch(EventsBatch<T> batch) {
        try {
            processEventExecutor.execute(() -> {
                prepareExecutor.processPrepare(batch);
                runOnFinishPrepareListener(batch);
                topicPartitionQueues.signalNotEmpty();
            });
            prepareEventBatches.add(batch);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("New batch " + batch.rawEventsCount + " (" + batch.rawEventsByteSize + " bytes), partitions: " + batch.offsetsToCommit.size() +
                        ", all prepare batches: " + prepareEventBatches.size() + ", sending: " + sendingEventBatches.size() +
                        " - " + batch.rawEvents.entrySet().stream().map(it -> it.getKey() + "=" + it.getValue().size()).collect(Collectors.joining(", "))
                );
            }

            metrics.updateBatch(batch.rawEventsCount, batch.rawEventsByteSize, batch.offsetsToCommit.size());

            isAnyChanged = true;
        } catch (RejectedExecutionException ex) {
            if (isRunning()) {
                LOGGER.error("Rejected exception in running state", ex);
            }
        }
    }

    /**
     * Send successfully prepared batches to sendExecutor
     */
    void sendBatches() {
        Iterator<EventsBatch<T>> iterator = prepareEventBatches.iterator();
        while (iterator.hasNext() && sendingEventBatches.size() < sendPoolSize) {
            EventsBatch<T> batch = iterator.next();

            if (noPartitionsAssigned(batch)) {
                iterator.remove();
                continue;
            }

            if (batch.isReadyToSend() && isAllowedToSend(batch)) {
                try {
                    sendEventExecutor.execute(() -> {
                        if (fakeSenderDelay == -1) {
                            sendExecutor.processSend(batch);
                        } else {
                            fakeDelaySender(batch);
                        }

                        topicPartitionQueues.signalNotEmpty();
                    });

                    sendingEventBatches.add(batch);
                    iterator.remove();
                    isAnyChanged = true;
                } catch (RejectedExecutionException ex) {
                    if (isRunning()) {
                        LOGGER.error("Rejected exception in running state", ex);
                    }
                }
            }
        }
    }

    /**
     * Sleep fakeSenderDelay and return ok result for testing
     *
     * @param batch batch
     */
    private void fakeDelaySender(EventsBatch<T> batch) {
        TimeSource.SYSTEM.sleep(fakeSenderDelay + (long) (fakeSenderDelay * new Random().nextDouble()));
        batch.setProcessorResult(ProcessorResult.ok(batch.getAllEvents().size(), 0));
    }

    /**
     * Checks that previous events from this partition have already been processed.<br>
     * Required to maintain consistent event handling.
     *
     * @param topicPartition TopicPartition
     * @return true if allowed to prepare
     */
    private boolean isAllowedToPrepare(TopicPartition topicPartition) {
        for (EventsBatch<T> eventsBatch : prepareEventBatches) {
            if (eventsBatch.offsetsToCommit.containsKey(topicPartition)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks that previous events from this partition have already been sended.<br>
     * Required to maintain consistent event handling.
     *
     * @param batch events batch
     * @return true if allowed to send
     */
    private boolean isAllowedToSend(EventsBatch<T> batch) {
        for (TopicPartition topicPartition : batch.offsetsToCommit.keySet()) {
            for (EventsBatch<T> sendingEventBatch : sendingEventBatches) {
                if (sendingEventBatch.offsetsToCommit.containsKey(topicPartition)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Finish processing events batches
     */
    void addToCommitAndRemoveProcessedBatches() {
        addToCommitAndRemoveProcessedBatches(prepareEventBatches);
        addToCommitAndRemoveProcessedBatches(sendingEventBatches);
    }

    /**
     * Finish processing events batches
     *
     * @param eventsBatches prepare or sending events batches
     */
    private void addToCommitAndRemoveProcessedBatches(Set<EventsBatch<T>> eventsBatches) {
        Iterator<EventsBatch<T>> iterator = eventsBatches.iterator();
        while (iterator.hasNext()) {
            EventsBatch<T> eventsBatch = iterator.next();
            if (eventsBatch.isProcessFinished()) {
                if (eventsBatch.getProcessorResult().isSuccess()) {
                    putOffsetsToCommit(eventsBatch.offsetsToCommit);
                }
                iterator.remove();
                metrics.update(eventsBatch.getProcessorResult());
                isAnyChanged = true;
            }
        }
    }

    private void runOnFinishPrepareListener(EventsBatch<T> eventsBatch) {
        try {
            eventsBatchListener.onFinishPrepare(eventsBatch);
        } catch (Exception ex) {
            LOGGER.error("Exception in eventsBatchListener on finish prepare", ex);
        }
    }

    /**
     * Add offsets to queue for commit in consumer thread
     *
     * @param offsets offsets from successfully sended events batch
     */
    private void putOffsetsToCommit(Map<TopicPartition, OffsetAndMetadata> offsets) {
        try {
            offsetsToCommitQueue.put(offsets);
        } catch (InterruptedException ex) {
            LOGGER.warn("Offsets queue put stopping was interrupted", ex);
        }
    }

    /**
     * Checking if a batch has no assigned topicPartition.<br>
     * Needed to speed up the stopping process revoked partitions
     *
     * @param eventsBatch events batch
     * @return true if topicPartitionQueues not contains any batch topicPartition
     */
    private boolean noPartitionsAssigned(EventsBatch<T> eventsBatch) {
        return eventsBatch.offsetsToCommit.keySet().stream()
                .noneMatch(topicPartition -> topicPartitionQueues.getPartitions().contains(topicPartition));
    }

    private boolean anyPartitionIsNoAssigned(EventsBatch<T> batch) {
        return batch.offsetsToCommit.keySet().stream()
                .anyMatch(topicPartition -> !topicPartitionQueues.getPartitions().contains(topicPartition));
    }

    private long getTotalByteSize() {
        long size = 0;
        for (EventsBatch<T> batch : prepareEventBatches) {
            size += batch.rawEventsByteSize;
        }
        for (EventsBatch<T> batch : sendingEventBatches) {
            size += batch.rawEventsByteSize;
        }
        return size;
    }

    /**
     * Waits until the sending of batches from the revoked partitions is finished
     *
     * @param partitions The list of partitions that were assigned to the consumer and now need to be revoked (may not
     *                   include all currently assigned partitions, i.e. there may still be some partitions left)
     * @see #isOnlyAssignedPartitionsInSend()
     */
    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        try {
            partitionsRevokedStarted = true;
            topicPartitionQueues.signalNotEmpty();

            while (isRunning() && partitionsRevokedStarted) {
                TimeUnit.MILLISECONDS.sleep(waitBatchesPeriodMs);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            LOGGER.error("Unspecified exception has been acquired", ex);
        }
    }

    /**
     * Checking if sending batches has only assigned partitions.
     */
    private void checkOnlyAssignedPartitionsInSend() {
        if (partitionsRevokedStarted && isOnlyAssignedPartitionsInSend()) {
            partitionsRevokedStarted = false;
        }
    }

    private boolean isOnlyAssignedPartitionsInSend() {
        long batchesWithNoActivePartitions = sendingEventBatches.stream()
                .filter(this::anyPartitionIsNoAssigned)
                .count();
        LOGGER.debug("Batches with no active partitions: " + batchesWithNoActivePartitions);
        return batchesWithNoActivePartitions == 0;
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        //do nothing
    }

    /**
     * Waits for all batches to be sent
     */
    void stopping() {
        try {
            while (!sendingEventBatches.isEmpty()) {
                addToCommitAndRemoveProcessedBatches(sendingEventBatches);
                TimeUnit.MILLISECONDS.sleep(waitBatchesPeriodMs);
                LOGGER.debug("Active batches: " + sendingEventBatches.size());
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            LOGGER.error("Unspecified exception has been acquired", ex);
        }
    }

    /**
     * Stop sender. Disable periodical update availability status.
     *
     * @param timeout  maximum time to wait
     * @param timeUnit time unit of the timeout
     * @return {@code true} if sender stopped normally, {@code false} if the timeout elapsed or the current thread was interrupted
     */
    @Override
    public boolean stop(long timeout, TimeUnit timeUnit) {
        running = false;
        topicPartitionQueues.signalNotEmpty();

        stopPrepare(timeout, timeUnit);

        try {
            if (sendEventExecutor != null) {
                sendEventExecutor.shutdown();
                if (!sendEventExecutor.awaitTermination(timeout, timeUnit)) {
                    sendEventExecutor.shutdownNow();
                    if (!sendEventExecutor.awaitTermination(timeout, timeUnit)) {
                        LOGGER.warn("Thread pool sendEventExecutor did not terminate");
                    }
                }
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping sendEventExecutor thread executor", t);
        }

        strategyExecutor.shutdown();
        try {
            if (!strategyExecutor.awaitTermination(timeout, timeUnit)) {
                strategyExecutor.shutdownNow();
                if (!strategyExecutor.awaitTermination(timeout, timeUnit)) {
                    LOGGER.warn("Thread pool did not terminate");
                }
                return false;
            }
            return true;
        } catch (InterruptedException e) {
            LOGGER.error("ParallelSenderStrategy shutdown execute was terminated by InterruptedException", e);
            return false;
        }
    }

    void stopPrepare(long timeout, TimeUnit timeUnit) {
        try {
            if (processEventExecutor != null) {
                processEventExecutor.shutdown();
                if (!processEventExecutor.awaitTermination(timeout, timeUnit)) {
                    processEventExecutor.shutdownNow();
                    if (!processEventExecutor.awaitTermination(timeout, timeUnit)) {
                        LOGGER.warn("Thread pool processEventExecutor did not terminate");
                    }
                }
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping processEventExecutor thread executor", t);
        }
    }

    /**
     * Check ParallelSenderStrategy running status.
     *
     * @return {@code true} if ParallelSenderStrategy is running and {@code false} if ParallelSender is stopping
     */
    private boolean isRunning() {
        return running;
    }

    static class Props {
        static final Parameter<Integer> PREPARE_POOL_SIZE =
                Parameter.integerParameter("preparePoolSize")
                        .withDefault(1)
                        .build();

        static final Parameter<Integer> SEND_POOL_SIZE =
                Parameter.integerParameter("sendPoolSize")
                        .withDefault(1)
                        .build();

        static final Parameter<Long> MAX_ALL_BATCHES_BYTE_SIZE =
                Parameter.longParameter("maxAllBatchesByteSize")
                        .withDefault(100_000_000L)
                        .build();

        static final Parameter<Long> WAIT_BATCHES_PERIOD_MS =
                Parameter.longParameter("waitBatchesPeriodMs")
                        .withDefault(100L)
                        .build();

        static final Parameter<Integer> FAKE_SENDER_DELAY =
                Parameter.integerParameter("fakeSenderDelay")
                        .withDefault(-1)
                        .build();
    }
}
