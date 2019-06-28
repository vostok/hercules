package ru.kontur.vostok.hercules.sink;

import org.jetbrains.annotations.NotNull;
import ru.kontur.vostok.hercules.protocol.Event;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public abstract class Processor {
    private volatile ProcessorStatus status = ProcessorStatus.AVAILABLE;
    private final Object mutex = new Object();

    /**
     * Return {@code true} if processor is available.
     *
     * @return {@code true} if processor is available, {@code false} otherwise
     */
    public final boolean isAvailable() {
        return status == ProcessorStatus.AVAILABLE;
    }

    /**
     * Wait for processor's availability
     *
     * @param timeoutMs maximum time (in millis) to wait
     * @return {@code true} if processor is available, {@code false} otherwise
     */
    public boolean awaitAvailability(long timeoutMs) {
        if (isAvailable()) {
            return true;
        }

        long nanoTime = System.nanoTime();
        long remainingTimeoutMs;
        synchronized (mutex) {
            if (isAvailable()) {
                return true;
            }
            while ((remainingTimeoutMs = timeoutMs - TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTime)) > 0
                    && !isAvailable()) {
                try {
                    mutex.wait(remainingTimeoutMs);
                } catch (InterruptedException e) {
                    /* Interruption during shutdown */
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        return isAvailable();

    }

    public abstract ProcessorResult process(List<Event> events);

    /**
     * Disable processor
     */
    protected final void disable() {
        status(ProcessorStatus.UNAVAILABLE);
    }

    /**
     * Enable processor
     */
    protected final void enable() {
        status(ProcessorStatus.AVAILABLE);
    }

    /**
     * Set processor status
     * @param status
     */
    protected final void status(@NotNull ProcessorStatus status) {
        synchronized (mutex) {
            this.status = status;
            mutex.notifyAll();
        }
    }
}
