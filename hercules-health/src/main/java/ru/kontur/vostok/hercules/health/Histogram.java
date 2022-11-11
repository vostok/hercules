package ru.kontur.vostok.hercules.health;

/**
 * A histogram metric calculates the distribution.
 * @author Gregory Koshelev
 */
public interface Histogram extends Metric {
    void update(int value);
    void update(long value);

    @Override
    default String name() {
        return "histogram";
    }
}
