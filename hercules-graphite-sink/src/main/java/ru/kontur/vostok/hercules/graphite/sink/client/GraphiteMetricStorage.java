package ru.kontur.vostok.hercules.graphite.sink.client;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class GraphiteMetricStorage {

    private final Map<String, List<GraphiteMetric>> metrics = new HashMap<>();

    public void add(String metricName, GraphiteMetric metric) {
        metrics.computeIfAbsent(metricName, s -> new LinkedList<>()).add(metric);
    }

    public Map<String, List<GraphiteMetric>> getMetrics() {
        return metrics;
    }
}
