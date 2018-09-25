package ru.kontur.vostok.hercules.graphite.client;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Temporal storage for graphite metrics
 */
public class GraphiteMetricStorage {

    private final Map<String, List<GraphiteMetricData>> metrics = new HashMap<>();

    public void add(String metricName, GraphiteMetricData metric) {
        metrics.computeIfAbsent(metricName, s -> new LinkedList<>()).add(metric);
    }

    public Map<String, List<GraphiteMetricData>> getMetrics() {
        return metrics;
    }
}
