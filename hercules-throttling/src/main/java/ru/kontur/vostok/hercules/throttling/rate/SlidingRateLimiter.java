package ru.kontur.vostok.hercules.throttling.rate;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Sliding rate limiter is based on event appearing rate.
 *
 * @author Gregory Koshelev
 */
public class SlidingRateLimiter {
    private final long limit;
    private final long timeWindowMs;
    private final AtomicLong value;
    private final AtomicLong lastUpdatedAt;

    public SlidingRateLimiter(long limit, long timeWindowMs, long startedAtMs) {
        this.limit = limit;
        this.timeWindowMs = timeWindowMs;
        this.value = new AtomicLong(limit * timeWindowMs);
        this.lastUpdatedAt = new AtomicLong(startedAtMs);
    }

    /**
     * Update rate and check if rate doesn't exceed limit.
     *
     * @param timeMs event time in milliseconds
     * @return {@code true} if rate doesn't exceed limit, otherwise {@code false}
     */
    public boolean updateAndCheck(long timeMs) {
        long currentLastUpdatedAt;
        do {
            currentLastUpdatedAt = lastUpdatedAt.get();
        } while (timeMs > currentLastUpdatedAt && !lastUpdatedAt.compareAndSet(currentLastUpdatedAt, timeMs));

        long deltaMs = Math.max(timeMs - currentLastUpdatedAt, 0);
        long increase = deltaMs * limit;
        long currentValue;
        long newValue;
        do {
            currentValue = value.get();
            newValue =  Math.min(currentValue + increase, limit * timeWindowMs);
        } while (!value.compareAndSet(currentValue, newValue));

        do {
            currentValue = value.get();
            newValue = currentValue - timeWindowMs;
        } while (newValue >= 0 && !value.compareAndSet(currentValue, newValue));

        return newValue >= 0;
    }
}
