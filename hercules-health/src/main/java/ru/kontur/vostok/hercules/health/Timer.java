package ru.kontur.vostok.hercules.health;

import java.util.concurrent.TimeUnit;

/**
 * Timer metric measures timing duration and throughput statistics.
 *
 * @author Gregory Koshelev
 */
public interface Timer extends Metric {
    /**
     * Update metric with duration.
     *
     * @param duration the duration
     * @param unit     the time unit
     */
    void update(long duration, TimeUnit unit);

    /**
     * Update metric with duration in milliseconds.
     *
     * @param durationMs the duration in milliseconds
     */
    default void update(long durationMs) {
        update(durationMs, TimeUnit.MILLISECONDS);
    }
}
