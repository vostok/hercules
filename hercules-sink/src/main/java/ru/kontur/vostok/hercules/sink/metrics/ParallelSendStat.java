package ru.kontur.vostok.hercules.sink.metrics;

import org.jetbrains.annotations.NotNull;
import ru.kontur.vostok.hercules.util.time.TimeSource;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Counts and stores {@link ru.kontur.vostok.hercules.sink.Sink Sink} metric values.
 * <br>
 * Used to update {@link SinkMetrics}.
 *
 * @author Anton Akkuzin
 */
public class ParallelSendStat {
    private final TimeSource time;

    private static final AtomicInteger sendingBatches = new AtomicInteger();

    private long processSendTimeMs;
    private long processSendStartedAtMs;

    public ParallelSendStat(@NotNull TimeSource time) {
        this.time = time;
    }

    /**
     * <p>Marks timestamp of the process send start.
     *
     * <p>After process end {@link ParallelSendStat#markProcessSendEnd()} should be called to measure process time.
     */
    public void markProcessSendStart() {
        processSendStartedAtMs = time.milliseconds();
        sendingBatches.incrementAndGet();
    }

    /**
     * Marks timestamp of the process send end. Total process time is a difference between
     * it and the {@code start timestamp}.
     */
    public void markProcessSendEnd() {
        processSendTimeMs = time.milliseconds() - processSendStartedAtMs;
        sendingBatches.decrementAndGet();
    }

    public long getProcessSendTimeMs() {
        return processSendTimeMs;
    }

    /**
     * Resets calculated values.
     */
    public void reset() {
        this.processSendTimeMs = 0;
    }

    public int getSendingBatches() {
        return sendingBatches.get();
    }
}
