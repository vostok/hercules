package ru.kontur.vostok.hercules.elastic.sink.metrics;

import org.apache.kafka.common.TopicPartition;
import ru.kontur.vostok.hercules.health.Meter;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.health.MetricsUtil;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Measure events by TopicWithIndex using {@link Meter} metrics.
 * <p>
 * The count of reported indices is limited. Oldest one metric would be removed if count exceeds the limit.
 *
 * @author Innokentiy Krivonosov
 */
public class TopicWithIndexMetricsCollector {
    private final String name;
    private final int indicesLimit;
    private final MetricsCollector metricsCollector;
    private final ConcurrentMap<TopicWithIndex, Meter> meters = new ConcurrentHashMap<>();
    private final AtomicInteger reportedIndicesCount = new AtomicInteger(0);
    private final ConcurrentLinkedQueue<TopicWithIndex> reportedIndices = new ConcurrentLinkedQueue<>();

    public TopicWithIndexMetricsCollector(String name, int indicesLimit, MetricsCollector metricsCollector) {
        this.name = name;
        this.indicesLimit = indicesLimit;
        this.metricsCollector = metricsCollector;

        metricsCollector.gauge(MetricsUtil.toMetricPath(name, "reportedIndicesCount"), reportedIndicesCount::get);
    }

    public void markEvent(TopicPartition topicPartition, String index) {
        eventsMeter(topicPartition, index).mark();
    }

    private Meter eventsMeter(TopicPartition topicPartition, String index) {
        TopicWithIndex topicWithIndex = new TopicWithIndex(topicPartition, index);
        Meter meter = meters.get(topicWithIndex);

        if (meter != null) {
            return meter;
        }

        return registerNew(topicWithIndex);
    }

    /**
     * Register new metric for TopicWithIndex.
     * <p>
     * If reported indices count exceeds the limit, then unregister the oldest metric.
     * <p>
     * The method is {@code synchronized} to avoid race condition
     * when the oldest TopicWithIndex is removed and at the same time it is added by another thread.
     *
     * @param topicWithIndex the TopicWithIndex
     * @return metric for TopicWithIndex
     */
    private synchronized Meter registerNew(TopicWithIndex topicWithIndex) {
        Meter newMeter = metricsCollector.meter(metricNameForIndex(topicWithIndex));

        Meter meter = meters.putIfAbsent(topicWithIndex, newMeter);
        if (meter != null) {
            return meter;
        }

        reportedIndices.add(topicWithIndex);
        if (reportedIndicesCount.incrementAndGet() > indicesLimit) {
            unregisterOld();
        }

        return newMeter;
    }

    private void unregisterOld() {
        TopicWithIndex index = reportedIndices.poll();
        if (index != null) {
            metricsCollector.remove(metricNameForIndex(index));
            meters.remove(index);
            reportedIndicesCount.decrementAndGet();
        }
    }

    private String metricNameForIndex(TopicWithIndex topicWithIndex) {
        return MetricsUtil.toMetricPath(name,
                "topic", topicWithIndex.topicPartition.topic(),
                "index", topicWithIndex.index,
                "events");
    }

    static class TopicWithIndex {
        private final TopicPartition topicPartition;
        private final String index;

        public TopicWithIndex(TopicPartition topicPartition, String index) {
            this.topicPartition = topicPartition;
            this.index = index;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TopicWithIndex that = (TopicWithIndex) o;
            return Objects.equals(topicPartition, that.topicPartition) && Objects.equals(index, that.index);
        }

        @Override
        public int hashCode() {
            return Objects.hash(topicPartition, index);
        }
    }
}
