package ru.kontur.vostok.hercules.util.time;

import ru.kontur.vostok.hercules.util.functional.ThrowableCallable;
import ru.kontur.vostok.hercules.util.functional.ThrowableRunnable;

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

    /**
     * Create timer with given timeout.
     *
     * @param timeoutMs Timeout in milliseconds.
     * @return Creted timer.
     */
    default Timer timer(long timeoutMs) {
        return new Timer(this, timeoutMs);
    }

    /**
     * Measure duration of execution of given {@link ThrowableRunnable runnable} object.
     *
     * @param runnable Runnable object.
     * @param <T>      Type of exception that can be thrown.
     * @return Duration of execution of {@link ThrowableRunnable#run()} method.
     * @throws T See implementation details of given runnable object.
     */
    default <T extends Throwable> long measureMs(ThrowableRunnable<T> runnable) throws T {
        long startedAtMs = milliseconds();
        runnable.run();
        return milliseconds() - startedAtMs;
    }

    /**
     * Measure duration of execution of given {@link ThrowableCallable callable} object.
     *
     * @param callable Callable object.
     * @param <T>      Type of exception that can be thrown.
     * @return Tuple of result {@link ThrowableCallable#call()} method and duration of execution.
     * @throws T See implementation details of given callable object.
     */
    default <T extends Throwable, R> Result<R> measureMs(ThrowableCallable<T, R> callable) throws T {
        long startedAtMs = milliseconds();
        R result = callable.call();
        return new Result<>(result, milliseconds() - startedAtMs);
    }

    /**
     * Tuple for result of measurements of {@link ThrowableCallable#call()}.
     *
     * @param <T> Result type of {@link ThrowableCallable#call()} method.
     */
    class Result<T> {

        private final T value;
        private final long durationMs;

        public Result(T value, long durationMs) {
            this.value = value;
            this.durationMs = durationMs;
        }

        /**
         * Get result of {@link ThrowableCallable#call()} method.
         *
         * @return {@link ThrowableCallable#call()} method result.
         */
        public T getValue() {
            return value;
        }

        /**
         * Get duration of execution of {@link ThrowableCallable#call()}.
         *
         * @return {@link ThrowableCallable#call()} method execution duration.
         */
        public long getDurationMs() {
            return durationMs;
        }
    }
}
