package ru.kontur.vostok.hercules.graphite.adapter.filter;

import ru.kontur.vostok.hercules.graphite.adapter.metric.Metric;

/**
 * The metric filter decides if a metric passes the filter or not.
 *
 * @author Gregory Koshelev
 */
public interface MetricFilter {
    /**
     * Test if the metric passes the filter or not.
     *
     * @param metric the metric
     * @return {@code true} if metric passes the filter or {@code false} if metric needs to be filtered out
     */
    boolean test(Metric metric);
}
