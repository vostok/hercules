package ru.kontur.vostok.hercules.health;

import java.util.function.Supplier;

/**
 * @author Innokentiy Krivonosov
 */
public interface IMetricsCollector {
    /**
     * Get throughput meter by the name
     *
     * @param name the name of the metric
     * @return requested meter
     */
    Meter meter(String name);

    /**
     * Get timer by the name
     *
     * @param name the name of the timer
     * @return requested timer
     */
    Timer timer(String name);

    /**
     * Get counter by the name
     *
     * @param name the name of the counter
     * @return requested counter
     */
    Counter counter(String name);

    /**
     * Get histogram by the name
     *
     * @param name the name of the histogram
     * @return requested histogram
     */
    Histogram histogram(String name);


    /**
     * Register metric by the name with custom function
     *
     * @param name     the name of the metric
     * @param supplier the custom function to provide metric's values
     * @param <T>      the metric's value type (ordinarily Integer or Long)
     */
    <T> void gauge(String name, Supplier<T> supplier);

    /**
     * Removes the metric with the given name
     *
     * @param name the name of the metric
     * @return whether or not the metric was removed
     */
    boolean remove(String name);
}
