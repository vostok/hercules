package ru.kontur.vostok.hercules.health;

/**
 * Incrementing and decrementing counter metric.
 *
 * @author Gregory Koshelev
 */
public interface Counter {
    /**
     * Increment by value.
     *
     * @param value value
     */
    void increment(long value);

    /**
     * Decrement by value.
     *
     * @param value value
     */
    void decrement(long value);

    /**
     * Increment by 1.
     */
    default void increment() {
        increment(1);
    }

    /**
     * Decrement by 1.
     */
    default void decrement() {
        decrement(1);
    }
}
