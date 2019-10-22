package ru.kontur.vostok.hercules.health;

/**
 * Meter metric measures mean and average throughput.
 * There is 1-minute, 5-minutes and 15-minutes moving average throughput.
 *
 * @author Gregory Koshelev
 */
public interface Meter extends Metric {
    /**
     * Mark meter with the value.
     *
     * @param n the value
     */
    void mark(long n);

    /**
     * Mark meter with 1.
     */
    default void mark() {
        mark(1);
    }
}
