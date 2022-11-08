package ru.kontur.vostok.hercules.sink.metrics;

import org.apache.kafka.common.TopicPartition;
import org.jetbrains.annotations.NotNull;
import ru.kontur.vostok.hercules.health.Meter;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.health.MetricsUtil;
import ru.kontur.vostok.hercules.health.Timer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores and sends {@link ru.kontur.vostok.hercules.sink.Sink Sink} metrics.
 *
 * @author Anton Akkuzin
 */
public class SinkMetrics {
    private static final String SINK_METRICS_SCOPE = SinkMetrics.class.getSimpleName();

    private final MetricsCollector metricsCollector;
    private final ConcurrentHashMap<String, ConsumerMetrics> consumerMetrics = new ConcurrentHashMap<>();

    private final Meter droppedEvents;
    private final Meter filteredEvents;
    private final Meter processedEvents;
    private final Meter rejectedEvents;
    private final Meter totalEvents;

    private final Meter totalProcessAttempts;
    private final Meter successProcessAttempts;

    private final Timer pollTimeMs;
    private final Timer filtrationTimeMs;
    private final Timer processTimeMs;

    private final Timer pollTimePerEventNs;
    private final Timer filtrationTimePerEventNs;
    private final Timer processTimePerEventNs;

    public SinkMetrics(@NotNull MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;

        this.droppedEvents = metricsCollector.meter("droppedEvents");
        this.filteredEvents = metricsCollector.meter("filteredEvents");
        this.processedEvents = metricsCollector.meter("processedEvents");
        this.rejectedEvents = metricsCollector.meter("rejectedEvents");
        this.totalEvents = metricsCollector.meter("totalEvents");

        this.totalProcessAttempts = metricsCollector.meter(metricPath("totalProcessAttempts"));
        this.successProcessAttempts = metricsCollector.meter(metricPath("successProcessAttempts"));

        this.pollTimeMs = metricsCollector.timer(metricPath("pollTimeMs"));
        this.filtrationTimeMs = metricsCollector.timer(metricPath("filtrationTimeMs"));
        this.processTimeMs = metricsCollector.timer(metricPath("processTimeMs"));

        this.pollTimePerEventNs = metricsCollector.timer(metricPath("pollTimePerEventNs"));
        this.filtrationTimePerEventNs = metricsCollector.timer(metricPath("filtrationTimePerEventNs"));
        this.processTimePerEventNs = metricsCollector.timer(metricPath("processTimePerEventNs"));
    }

    /**
     * Updates metric values by provided {@link Stat}.
     *
     * @param stat {@link Stat} contains calculated metric values
     */
    public void update(@NotNull Stat stat) {
        this.droppedEvents.mark(stat.getDroppedEvents());
        this.filteredEvents.mark(stat.getFilteredEvents());
        this.processedEvents.mark(stat.getProcessedEvents());
        this.rejectedEvents.mark(stat.getRejectedEvents());
        this.totalEvents.mark(stat.getTotalEvents());

        this.totalProcessAttempts.mark();
        if (stat.isProcessSucceed()) {
            this.successProcessAttempts.mark();
        }

        this.pollTimeMs.update(stat.getPollTimeMs());
        this.filtrationTimeMs.update(stat.getFiltrationTimeMs());
        this.processTimeMs.update(stat.getProcessTimeMs());

        this.pollTimePerEventNs.update(stat.getPollTimePerEventNs());
        this.filtrationTimePerEventNs.update(stat.getFiltrationTimePerEventNs());
        this.processTimePerEventNs.update(stat.getProcessTimePerEventNs());

        consumerMetrics.computeIfAbsent(stat.getConsumerId(),
                consumerId -> new ConsumerMetrics(metricsCollector, consumerId)).update(stat);
    }

    /**
     * @return inner {@link MetricsCollector}
     */
    public MetricsCollector getMetricsCollector() {
        return metricsCollector;
    }

    private String metricPath(String metricName) {
        return MetricsUtil.toMetricPath(SINK_METRICS_SCOPE, metricName);
    }

    /**
     * Stores and sends {@link ru.kontur.vostok.hercules.sink.Sink Sink} metrics by Kafka-consumers.
     */
    private static class ConsumerMetrics {
        private final String consumerMetricsScope;

        private final MetricsCollector metricsCollector;

        private final Timer pollTimeMs;
        private final Timer processTimeMs;
        private final Meter totalEvents;

        private final Map<TopicPartition, Meter> totalEventsPerPartition = new HashMap<>();
        private final Map<TopicPartition, Meter> batchSizePerPartition = new HashMap<>();

        private ConsumerMetrics(@NotNull MetricsCollector metricsCollector, String consumerId) {
            this.consumerMetricsScope = MetricsUtil.toMetricPath(SINK_METRICS_SCOPE, ConsumerMetrics.class.getSimpleName(), consumerId);
            this.metricsCollector = metricsCollector;
            this.pollTimeMs = metricsCollector.timer(metricPath( "pollTimeMs"));
            this.processTimeMs = metricsCollector.timer(metricPath( "processTimeMs"));
            this.totalEvents = metricsCollector.meter(metricPath( "totalEvents"));
        }

        private String metricPath(String... metricNameParts) {
            return MetricsUtil.toMetricPathWithPrefix(consumerMetricsScope, metricNameParts);
        }

        /**
         * Updates metric values by provided {@link Stat}.
         *
         * @param stat {@link Stat} contains calculated metric values
         */
        private void update(@NotNull Stat stat) {
            this.pollTimeMs.update(stat.getPollTimeMs());
            this.processTimeMs.update(stat.getProcessTimeMs());
            this.totalEvents.mark(stat.getTotalEvents());

            updatePerPartitionMetrics(stat.getTotalEventsPerPartition(), totalEventsPerPartition, "totalEvents");
            updatePerPartitionMetrics(stat.getBatchSizePerPartition(), batchSizePerPartition, "batchSize");

            clearUnsubscribedPartitionsMetrics(stat, totalEventsPerPartition);
            clearUnsubscribedPartitionsMetrics(stat, batchSizePerPartition);
        }

        private void updatePerPartitionMetrics(Map<TopicPartition, Integer> data, Map<TopicPartition, Meter> metrics, String metricName) {
            for (Map.Entry<TopicPartition, Integer> entry : data.entrySet()) {
                metrics.computeIfAbsent(
                                entry.getKey(),
                                p -> metricsCollector.meter(metricPath("partition", p.toString(), metricName)))
                        .mark(entry.getValue());
            }
        }

        private void clearUnsubscribedPartitionsMetrics(Stat stat, Map<TopicPartition, Meter> metrics) {
            Set<TopicPartition> currentPartitionAssignment = stat.getCurrentPartitionAssignment();
            for (Map.Entry<TopicPartition, Meter> entry : metrics.entrySet()) {
                if (!currentPartitionAssignment.contains(entry.getKey())) {
                    metricsCollector.remove(entry.getValue().name());
                    metrics.remove(entry.getKey());
                }
            }

        }
    }
}
