package ru.kontur.vostok.hercules.util.time;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Mock the clock.
 * <p>
 * This time source should be used in unit testing of time-specific algorithms with full control over the clock.
 * <p>
 * This class doesn't suit for multi-threading testing: see javadoc for {@link #sleep(long)} method.
 *
 * @author Gregory Koshelev
 */
public class MockTimeSource implements TimeSource {
    private final AtomicLong milliseconds;
    private final AtomicLong nanoseconds;

    /**
     * The clock starts with current time of the system clock.
     */
    public MockTimeSource() {
        this(System.currentTimeMillis(), System.nanoTime());
    }

    /**
     * The clock starts with the specified time.
     *
     * @param currentMillis the current time in milliseconds
     * @param currentNanos  the current time in nanoseconds
     */
    public MockTimeSource(long currentMillis, long currentNanos) {
        this.milliseconds = new AtomicLong(currentMillis);
        this.nanoseconds = new AtomicLong(currentNanos);
    }

    @Override
    public long milliseconds() {
        return milliseconds.get();
    }

    @Override
    public long nanoseconds() {
        return nanoseconds.get();
    }

    /**
     * Fake sleep is only moves the clock forward for the specified time.
     * <p>
     * Thus, calling this method from different threads yields the clock is moved forward for multiple times.
     *
     * @param millis the time to sleep in milliseconds
     */
    @Override
    public void sleep(long millis) {
        if (millis < 0) {
            throw new IllegalArgumentException("Argument must be non-negative");
        }
        milliseconds.addAndGet(millis);
        nanoseconds.addAndGet(TimeUnit.MILLISECONDS.toNanos(millis));
    }
}
