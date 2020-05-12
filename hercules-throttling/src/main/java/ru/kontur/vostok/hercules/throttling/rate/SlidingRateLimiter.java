package ru.kontur.vostok.hercules.throttling.rate;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Sliding rate limiter is used to throttle events by rate. Throttling is based on event appearing rate.
 * <p>
 * Algorithm is based on token bucket algorithm.
 *
 * @author Gregory Koshelev
 */
public class SlidingRateLimiter {
    private final long limit;
    private final long maximumTokens;
    private final long eventTokens;

    private final AtomicLong availableTokens;
    private final AtomicLong updatedAtMs;

    /**
     * Rate limiter accepts no more than {@code limit} events for every {@code timeWindowsMs} milliseconds. Thus,
     * <pre>
     * rate <= limit / timeWindowMs
     * </pre>
     *
     * @param limit        the maximum events per window of {@code timeWindowMs} milliseconds
     * @param timeWindowMs the time window is used to compute rate
     * @param startedAtMs  initialization timestamp in milliseconds (e.g. first event timestamp)
     */
    public SlidingRateLimiter(long limit, long timeWindowMs, long startedAtMs) {
        this.limit = limit;
        this.maximumTokens = limit * timeWindowMs;
        this.eventTokens = timeWindowMs;

        this.availableTokens = new AtomicLong(maximumTokens);
        this.updatedAtMs = new AtomicLong(startedAtMs);
    }

    /**
     * Update rate and check if rate doesn't exceed limit.
     *
     * @param timeMs event timestamp in milliseconds
     * @return {@code true} if rate doesn't exceed limit, otherwise {@code false}
     */
    public boolean updateAndCheck(long timeMs) {
        long currentUpdatedAtMs;
        do {
            currentUpdatedAtMs = updatedAtMs.get();
        } while (timeMs > currentUpdatedAtMs && !updatedAtMs.compareAndSet(currentUpdatedAtMs, timeMs));

        long deltaMs = Math.max(timeMs - currentUpdatedAtMs, 0);
        long increaseTokens = deltaMs * limit;
        long currentAvailableTokens;
        long newAvailableTokens;
        do {
            currentAvailableTokens = availableTokens.get();
            newAvailableTokens = Math.min(currentAvailableTokens + increaseTokens, maximumTokens);
        } while (!availableTokens.compareAndSet(currentAvailableTokens, newAvailableTokens));

        do {
            currentAvailableTokens = availableTokens.get();
            newAvailableTokens = currentAvailableTokens - eventTokens;
        } while (newAvailableTokens >= 0 && !availableTokens.compareAndSet(currentAvailableTokens, newAvailableTokens));

        return newAvailableTokens >= 0;
    }
}
