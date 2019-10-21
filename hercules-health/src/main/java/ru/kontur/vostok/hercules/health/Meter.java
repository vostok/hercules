package ru.kontur.vostok.hercules.health;

/**
 * @author Gregory Koshelev
 */
public interface Meter extends Metric {
    void mark(long n);

    default void mark() {
        mark(1);
    }
}
