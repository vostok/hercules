package ru.kontur.vostok.hercules.sink.metrics;

import org.apache.kafka.common.TopicPartition;
import org.jetbrains.annotations.NotNull;
import ru.kontur.vostok.hercules.sink.ProcessorResult;
import ru.kontur.vostok.hercules.util.time.TimeSource;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Counts and stores {@link ru.kontur.vostok.hercules.sink.Sink Sink} metric values.
 * <br>
 * Used to update {@link SinkMetrics}.
 *
 * @author Anton Akkuzin
 */
public class Stat {

    private final String consumerId;
    private final TimeSource time;
    private final Supplier<Set<TopicPartition>> currentAssignmentSupplier;

    private int droppedEvents;
    private int filteredEvents;
    private int totalEvents;

    private long pollTimeMs;
    private long pollStartedAtMs;

    private long filtrationTimeMs;
    private long filtrationStartedAtMs;

    private long processTimeMs;
    private long processStartedAtMs;

    private ProcessorResult processorResult;

    private final Map<TopicPartition, Integer> totalEventsPerPartition = new HashMap<>();
    private final Map<TopicPartition, Integer> batchSizePerPartition = new HashMap<>();

    public Stat(String consumerId, @NotNull TimeSource time, Supplier<Set<TopicPartition>> currentAssignmentSupplier) {
        this.consumerId = consumerId;
        this.time = time;
        this.currentAssignmentSupplier = currentAssignmentSupplier;
    }

    public void incrementTotalEventsPerPartition(TopicPartition partition) {
        totalEventsPerPartition.merge(partition, 1, Integer::sum);
    }

    public void incrementBatchSizePerPartition(TopicPartition partition, int eventSize) {
        batchSizePerPartition.merge(partition, eventSize, Integer::sum);
    }

    /**
     * <p>Marks timestamp of the current poll iteration start.
     *
     * <p>After poll end {@link Stat#markPollEnd()} should be called to measure current iteration time.
     */
    public void markPollStart() {
        pollStartedAtMs = time.milliseconds();
    }

    /**
     * Marks timestamp of the poll iteration end. Adds the difference between
     * it and the {@code start timestamp} to the {@code total poll time}.
     */
    public void markPollEnd() {
        pollTimeMs += time.milliseconds() - pollStartedAtMs;
    }

    /**
     * <p>Marks timestamp of the current filtration iteration start.
     *
     * <p>After filtration end {@link Stat#markFiltrationEnd()} should be called to measure current iteration time.
     */
    public void markFiltrationStart() {
        filtrationStartedAtMs = time.milliseconds();
    }

    /**
     * Marks timestamp of the filtration iteration end. Adds the difference between
     * it and the {@code start timestamp} to the {@code total filtration time}.
     */
    public void markFiltrationEnd() {
        filtrationTimeMs += time.milliseconds() - filtrationStartedAtMs;
    }

    /**
     * <p>Marks timestamp of the process start.
     *
     * <p>After process end {@link Stat#markProcessEnd()} should be called to measure process time.
     */
    public void markProcessStart() {
        processStartedAtMs = time.milliseconds();
    }

    /**
     * Marks timestamp of the process end. Total process time is a difference between
     * it and the {@code start timestamp}.
     */
    public void markProcessEnd() {
        processTimeMs = time.milliseconds() - processStartedAtMs;
    }

    /**
     * Resets calculated values.
     */
    public void reset() {
        this.droppedEvents = 0;
        this.filteredEvents = 0;
        this.totalEvents = 0;
        this.pollTimeMs = 0;
        this.filtrationTimeMs = 0;
        this.processTimeMs = 0;
        this.totalEventsPerPartition.clear();
        this.batchSizePerPartition.clear();
    }

    public void incrementDroppedEvents() {
        this.droppedEvents++;
    }

    public void incrementFilteredEvents() {
        this.filteredEvents++;
    }

    public void setTotalEvents(int totalEvents) {
        this.totalEvents = totalEvents;
    }

    public void setProcessorResult(ProcessorResult result) {
        this.processorResult = result;
    }

    public String getConsumerId() {
        return consumerId;
    }

    public int getDroppedEvents() {
        return droppedEvents;
    }

    public int getFilteredEvents() {
        return filteredEvents;
    }

    public int getProcessedEvents() {
        return processorResult.getProcessedEvents();
    }

    public int getRejectedEvents() {
        return processorResult.getRejectedEvents();
    }

    public int getTotalEvents() {
        return totalEvents;
    }

    public long getPollTimeMs() {
        return pollTimeMs;
    }

    public long getFiltrationTimeMs() {
        return filtrationTimeMs;
    }

    public long getProcessTimeMs() {
        return processTimeMs;
    }

    public boolean isProcessSucceed() {
        return processorResult.isSuccess();
    }

    public Map<TopicPartition, Integer> getTotalEventsPerPartition() {
        return totalEventsPerPartition;
    }

    public Map<TopicPartition, Integer> getBatchSizePerPartition() {
        return batchSizePerPartition;
    }

    public long getPollTimePerEventNs() {
        return calculatePerEventTime(pollTimeMs, totalEvents + filteredEvents + droppedEvents);
    }

    public long getFiltrationTimePerEventNs() {
        return calculatePerEventTime(filtrationTimeMs, totalEvents + filteredEvents);
    }

    public long getProcessTimePerEventNs() {
        return calculatePerEventTime(processTimeMs, totalEvents);
    }

    private long calculatePerEventTime(long timeMs, int events) {
        return events != 0 ? TimeUtil.millisToNanos(timeMs) / events : 0;
    }

    public Set<TopicPartition> getCurrentPartitionAssignment() {
        return currentAssignmentSupplier.get();
    }
}
