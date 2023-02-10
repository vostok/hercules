package ru.kontur.vostok.hercules.sink.metrics;

import org.jetbrains.annotations.NotNull;
import ru.kontur.vostok.hercules.util.time.TimeSource;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

/**
 * Counts and stores {@link ru.kontur.vostok.hercules.sink.parallel.ParallelSenderStrategy Sink} metric values.
 * <br>
 * Used to update {@link SinkMetrics}.
 *
 * @author Innokentiy Krivonosov
 */
public class ParallelPrepareStat {

    private final TimeSource time;

    private int droppedEvents;
    private int filteredEvents;
    private int totalEvents;

    private long deserializationTimeMs;
    private long deserializationStartedAtMs;

    private long filtrationTimeMs;
    private long filtrationStartedAtMs;

    private long processTimeMs;
    private long processStartedAtMs;

    public ParallelPrepareStat(@NotNull TimeSource time) {
        this.time = time;
    }

    /**
     * <p>Marks timestamp of the current deserialization iteration start.
     *
     * <p>After deserialization end {@link ParallelPrepareStat#markDeserializationEnd()} should be called to measure current iteration time.
     */
    public void markDeserializationStart() {
        deserializationStartedAtMs = time.milliseconds();
    }

    /**
     * Marks timestamp of the deserialization iteration end. Adds the difference between
     * it and the {@code start timestamp} to the {@code total deserialization time}.
     */
    public void markDeserializationEnd() {
        deserializationTimeMs += time.milliseconds() - deserializationStartedAtMs;
    }

    /**
     * <p>Marks timestamp of the current filtration iteration start.
     *
     * <p>After filtration end {@link ParallelPrepareStat#markFiltrationEnd()} should be called to measure current iteration time.
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
     * <p>After process end {@link ParallelPrepareStat#markProcessEnd()} should be called to measure process time.
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
        this.deserializationTimeMs = 0;
        this.filtrationTimeMs = 0;
        this.processTimeMs = 0;
    }

    public int getTotalEvents() {
        return totalEvents;
    }

    public void setTotalEvents(int totalEvents) {
        this.totalEvents = totalEvents;
    }

    public void incrementDroppedEvents() {
        this.droppedEvents++;
    }

    public void incrementDroppedEvents(int count) {
        this.droppedEvents += count;
    }

    public int getDroppedEvents() {
        return droppedEvents;
    }

    public void incrementFilteredEvents() {
        this.filteredEvents++;
    }

    public int getFilteredEvents() {
        return filteredEvents;
    }

    public long getDeserializationTimeMs() {
        return deserializationTimeMs;
    }

    public long getFiltrationTimeMs() {
        return filtrationTimeMs;
    }

    public long getProcessTimeMs() {
        return processTimeMs;
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
