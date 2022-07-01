package ru.kontur.vostok.hercules.sink.metrics;

import org.jetbrains.annotations.NotNull;
import ru.kontur.vostok.hercules.sink.ProcessorResult;
import ru.kontur.vostok.hercules.util.time.TimeSource;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

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

    public Stat(String consumerId, @NotNull TimeSource time) {
        this.consumerId = consumerId;
        this.time = time;
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
}
