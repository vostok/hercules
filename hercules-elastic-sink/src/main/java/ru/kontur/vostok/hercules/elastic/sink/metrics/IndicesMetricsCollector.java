package ru.kontur.vostok.hercules.elastic.sink.metrics;

import ru.kontur.vostok.hercules.health.IMetricsCollector;
import ru.kontur.vostok.hercules.health.Meter;
import ru.kontur.vostok.hercules.health.MetricsUtil;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Measure events by index using {@link Meter} metrics.
 * <p>
 * The count of reported indices is limited. Oldest one metric would be removed if count exceeds the limit.
 *
 * @author Gregory Koshelev
 */
public class IndicesMetricsCollector {
    private final String name;
    private final int indicesLimit;
    private final IMetricsCollector metricsCollector;
    private final ConcurrentMap<String, Meter> meters = new ConcurrentHashMap<>();
    private final AtomicInteger reportedIndicesCount = new AtomicInteger(0);
    private final ConcurrentLinkedQueue<String> reportedIndices = new ConcurrentLinkedQueue<>();

    public IndicesMetricsCollector(String name, int indicesLimit, IMetricsCollector metricsCollector) {
        this.name = name;
        this.indicesLimit = indicesLimit;
        this.metricsCollector = metricsCollector;

        metricsCollector.gauge(MetricsUtil.toMetricPath(name, "reportedIndicesCount"), reportedIndicesCount::get);
    }

    public void markEvent(String index) {
        eventsMeter(index).mark();
    }

    private Meter eventsMeter(String index) {
        Meter meter = meters.get(index);

        if (meter != null) {
            return meter;
        }

        return registerNew(index);
    }

    /**
     * Register new metric for index.
     * <p>
     * If reported indices count exceeds the limit, then unregister the oldest metric.
     * <p>
     * The method is {@code synchronized} to avoid race condition
     * when the oldest index is removed and at the same time it is added by another thread.
     *
     * @param index the index
     * @return metric for index
     */
    private synchronized Meter registerNew(String index) {
        Meter newMeter = metricsCollector.meter(metricNameForIndex(index));

        Meter meter = meters.putIfAbsent(index, newMeter);
        if (meter != null) {
            return meter;
        }

        reportedIndices.add(index);
        if (reportedIndicesCount.incrementAndGet() > indicesLimit) {
            unregisterOld();
        }

        return newMeter;
    }

    private void unregisterOld() {
        String index = reportedIndices.poll();
        if (index != null) {
            metricsCollector.remove(metricNameForIndex(index));
            meters.remove(index);
            reportedIndicesCount.decrementAndGet();
        }
    }

    private String metricNameForIndex(String index) {
        return MetricsUtil.toMetricPath(name, "byIndex", index, "events");
    }
}
