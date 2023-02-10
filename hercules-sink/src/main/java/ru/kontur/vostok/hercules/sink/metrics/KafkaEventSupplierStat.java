package ru.kontur.vostok.hercules.sink.metrics;

import org.apache.kafka.common.TopicPartition;
import org.jetbrains.annotations.NotNull;
import ru.kontur.vostok.hercules.sink.parallel.ParallelEventConsumer;
import ru.kontur.vostok.hercules.util.time.TimeSource;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Counts and stores {@link ParallelEventConsumer Sink} metric values.
 * <br>
 * Used to update {@link SinkMetrics}.
 *
 * @author Innokentiy Krivonosov
 */
public class KafkaEventSupplierStat implements SinkMetrics.CurrentPartitionAssignmentSupplier {

    private final TimeSource time;
    private final Supplier<Set<TopicPartition>> currentAssignmentSupplier;

    private int pollEventsCount;
    private long totalEvents;
    private long pollTimeMs;
    private long pollStartedAtMs;

    private final Map<TopicPartition, Integer> totalEventsPerPartition = new HashMap<>();
    private final Map<TopicPartition, Integer> batchSizePerPartition = new HashMap<>();

    public KafkaEventSupplierStat(@NotNull TimeSource time, Supplier<Set<TopicPartition>> currentAssignmentSupplier) {
        this.time = time;
        this.currentAssignmentSupplier = currentAssignmentSupplier;
    }

    /**
     * <p>Marks timestamp of the current poll iteration start.
     *
     * <p>After poll end {@link KafkaEventSupplierStat#markPollEnd(int events)} should be called to measure current iteration time.
     */
    public void markPollStart() {
        pollStartedAtMs = time.milliseconds();
    }

    /**
     * Marks timestamp of the poll iteration end. Adds the difference between
     * it and the {@code start timestamp} to the {@code total poll time}.
     */
    public void markPollEnd(int pollEventsCount) {
        pollTimeMs = time.milliseconds() - pollStartedAtMs;
        this.pollEventsCount = pollEventsCount;
    }

    /**
     * Resets calculated values.
     */
    public void reset() {
        this.totalEventsPerPartition.clear();
        this.batchSizePerPartition.clear();
    }

    public long getPollTimeMs() {
        return pollTimeMs;
    }

    public long getPollTimePerEventNs() {
        return calculatePerEventTime(pollTimeMs, pollEventsCount);
    }

    private long calculatePerEventTime(long timeMs, int events) {
        return events != 0 ? TimeUtil.millisToNanos(timeMs) / events : 0;
    }

    public void incrementTotalEventsPerPartition(TopicPartition partition, int count) {
        totalEventsPerPartition.merge(partition, count, Integer::sum);
    }

    public void incrementBatchSizePerPartition(TopicPartition partition, int eventSize) {
        batchSizePerPartition.merge(partition, eventSize, Integer::sum);
    }

    public Map<TopicPartition, Integer> getTotalEventsPerPartition() {
        return totalEventsPerPartition;
    }

    public Map<TopicPartition, Integer> getBatchSizePerPartition() {
        return batchSizePerPartition;
    }

    @Override
    public Set<TopicPartition> getCurrentPartitionAssignment() {
        return currentAssignmentSupplier.get();
    }

    public long getTotalEvents() {
        return totalEvents;
    }

    public void setTotalEventCount(long totalEvents) {
        this.totalEvents = totalEvents;
    }

    public int getPollEventsCount() {
        return pollEventsCount;
    }
}
