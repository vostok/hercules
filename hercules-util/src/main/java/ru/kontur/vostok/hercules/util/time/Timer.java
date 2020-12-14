package ru.kontur.vostok.hercules.util.time;

import java.time.Duration;

/**
 * Timer measures elapsed and remaining time.
 * <p>
 * Timer is reusable and not thread-safe. Thus, per thread instance should be used.
 *
 * @author Gregory Koshelev
 */
public class Timer {
    private final TimeSource time;
    private final long timeoutMs;
    private long startedAtMs;

    Timer(TimeSource time, long timeoutMs) {
        this.time = time;
        this.timeoutMs = timeoutMs;
        this.startedAtMs = time.milliseconds();
    }

    public Timer(long timeoutMs) {
        this(TimeSource.SYSTEM, timeoutMs);
    }

    /**
     * Returns timer's timeout in millis
     *
     * @return timeout in millis
     */
    public long timeoutMs() {
        return timeoutMs;
    }

    /**
     * Returns elapsed time in millis.
     * <p>
     * Elapsed time is measured since timer creation or last {@link #reset()}.
     *
     * @return elapsed time in millis
     */
    public long elapsedTimeMs() {
        return time.milliseconds() - startedAtMs;
    }

    /**
     * Returns remaining time in millis at least zero.
     * <p>
     * Remaining time is non-negative so it can be safely used as timeout for new timers or operations with timeouts.<br>
     * Note, be careful when using {@link Object#wait(long)} as it waits indefinitely if timeout equals zero.
     *
     * @return remaining time in millis at least zero
     */
    public long remainingTimeMs() {
        return Math.max(timeoutMs - elapsedTimeMs(), 0L);
    }

    /**
     * Returns remaining time as {@link Duration}.
     *
     * @return remaining time
     */
    public Duration toDuration() {
        return Duration.ofMillis(remainingTimeMs());
    }

    /**
     * Checks if timer is expired.
     * <p>
     * Timer is expired if no remaining time left (i.e. {@link #remainingTimeMs()} equals zero).
     *
     * @return {@code true} if timer is expired
     */
    public boolean isExpired() {
        return remainingTimeMs() <= 0;
    }

    public void reset() {
        this.startedAtMs = time.milliseconds();
    }
}
