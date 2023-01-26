package ru.kontur.vostok.hercules.opentelemetry.adapter.metrics;

import ru.kontur.vostok.hercules.health.Meter;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.health.MetricsUtil;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Measure events by <code>service.name</code> using {@link Meter} metrics.
 * <p>
 * <code>service.name</code> is required attribute in OpenTelemetry
 * <p>
 * The count of reported indices is limited. Oldest one metric would be removed if count exceeds the limit.
 * <p>
 *
 * @author Innokentiy Krivonosov
 * @see <a href="https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/resource/semantic_conventions/README.md">
 * Resource Semantic Conventions</a>
 */
public class ServiceNameMetricsCollector {
    private final String telemetryType;
    private final String name;
    private final int indicesLimit;
    private final MetricsCollector metricsCollector;
    private final ConcurrentMap<String, Meter> meters = new ConcurrentHashMap<>();
    private int reportedIndicesCount = 0;
    private final Queue<String> reportedIndices = new ArrayDeque<>();

    public ServiceNameMetricsCollector(String name, String telemetryType, int indicesLimit, MetricsCollector metricsCollector) {
        this.name = name;
        this.telemetryType = telemetryType;
        this.indicesLimit = indicesLimit;
        this.metricsCollector = metricsCollector;
    }

    public void markEvent(String serviceName, int count) {
        eventsMeter(serviceName).mark(count);
    }

    private Meter eventsMeter(String serviceName) {
        Meter meter = meters.get(serviceName);

        if (meter != null) {
            return meter;
        }

        return registerNew(serviceName);
    }

    /**
     * Register new metric for serviceName.
     * <p>
     * If reported indices count exceeds the limit, then unregister the oldest metric.
     * <p>
     * The method is {@code synchronized} to avoid race condition
     * when the oldest serviceName is removed and at the same time it is added by another thread.
     *
     * @param serviceName the serviceName
     * @return metric for serviceName
     */
    private synchronized Meter registerNew(String serviceName) {
        Meter newMeter = metricsCollector.meter(metricNameForServiceName(serviceName));

        Meter meter = meters.putIfAbsent(serviceName, newMeter);
        if (meter != null) {
            return meter;
        }

        reportedIndices.add(serviceName);
        if (++reportedIndicesCount > indicesLimit) {
            unregisterOld();
        }

        return newMeter;
    }

    private void unregisterOld() {
        String serviceName = reportedIndices.poll();
        if (serviceName != null) {
            metricsCollector.remove(metricNameForServiceName(serviceName));
            meters.remove(serviceName);
            reportedIndicesCount--;
        }
    }

    private String metricNameForServiceName(String serviceName) {
        return MetricsUtil.toMetricPath(name, "telemetryType", telemetryType, "serviceName", serviceName, "events");
    }
}
