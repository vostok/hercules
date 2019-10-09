package ru.kontur.vostok.hercules.util.time;

/**
 * Abstracting the clock to use in unit testing of time-specific algorithms.
 *
 * @author Gregory Koshelev
 */
public interface TimeSource {
    TimeSource SYSTEM = new SystemTimeSource();

    /**
     * Returns the current system time in milliseconds.
     *
     * @return the time in milliseconds
     * @see System#currentTimeMillis()
     */
    long milliseconds();

    /**
     * Returns the current high-resolution time in nanoseconds.
     * <p>
     * Should be used to measure elapsed time.
     *
     * @return the time in nanoseconds
     * @see System#nanoTime()
     */
    long nanoseconds();

    /**
     * Sleep for the specified number of milliseconds.
     * <p>
     * The value of {@code millis} must be non-negative.
     *
     * @param millis the time to sleep in milliseconds
     * @throws IllegalArgumentException if the value of {@code millis} is negative
     * @see Thread#sleep(long)
     */
    void sleep(long millis);
}
