package ru.kontur.vostok.hercules.sink.metrics;

import org.apache.kafka.common.TopicPartition;
import org.jetbrains.annotations.NotNull;
import ru.kontur.vostok.hercules.health.Histogram;
import ru.kontur.vostok.hercules.health.IMetricsCollector;
import ru.kontur.vostok.hercules.health.Meter;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.health.MetricsUtil;
import ru.kontur.vostok.hercules.health.Timer;
import ru.kontur.vostok.hercules.sink.ProcessorResult;

import java.util.HashMap;
import java.util.Iterator;
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

    private final IMetricsCollector metricsCollector;
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

    /**
     * Parallel sink metrics
     */
    private final Timer deserializationTimeMs;
    private final Timer processSendTimeMs;
    private final Histogram batchEvents;
    private final Histogram batchByteSize;
    private final Map<Integer, Meter> batchPartitionsCount = new HashMap<>();

    public final Histogram pollEvents;
    public final Histogram totalQueueEvents;
    public final Histogram sendingBatchesCount;

    public SinkMetrics(@NotNull IMetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;

        this.droppedEvents = metricsCollector.meter("droppedEvents");
        this.filteredEvents = metricsCollector.meter("filteredEvents");
        this.processedEvents = metricsCollector.meter("processedEvents");
        this.rejectedEvents = metricsCollector.meter("rejectedEvents");
        this.totalEvents = metricsCollector.meter("totalEvents");

        this.totalProcessAttempts = metricsCollector.meter(metricPath("totalProcessAttempts"));
        this.successProcessAttempts = metricsCollector.meter(metricPath("successProcessAttempts"));

        this.pollTimeMs = metricsCollector.timer(metricPath("pollTimeMs"));
        this.deserializationTimeMs = metricsCollector.timer(metricPath("deserializationTimeMs"));
        this.filtrationTimeMs = metricsCollector.timer(metricPath("filtrationTimeMs"));
        this.processTimeMs = metricsCollector.timer(metricPath("processTimeMs"));
        this.processSendTimeMs = metricsCollector.timer(metricPath("processSendTimeMs"));

        this.pollTimePerEventNs = metricsCollector.timer(metricPath("pollTimePerEventNs"));
        this.filtrationTimePerEventNs = metricsCollector.timer(metricPath("filtrationTimePerEventNs"));
        this.processTimePerEventNs = metricsCollector.timer(metricPath("processTimePerEventNs"));

        this.pollEvents = metricsCollector.histogram(metricPath("pollEvents"));
        this.totalQueueEvents = metricsCollector.histogram(metricPath("totalQueueEvents"));
        this.batchEvents = metricsCollector.histogram(metricPath("batchEvents"));
        this.batchByteSize = metricsCollector.histogram(metricPath("batchByteSize"));
        this.sendingBatchesCount = metricsCollector.histogram(metricPath("sendingBatchesCount"));
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
        this.batchEvents.update(stat.getTotalEvents());

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

    public void update(KafkaEventSupplierStat stat) {
        this.pollTimeMs.update(stat.getPollTimeMs());
        this.pollTimePerEventNs.update(stat.getPollTimePerEventNs());

        this.pollEvents.update(stat.getPollEventsCount());
        this.totalQueueEvents.update(stat.getTotalEvents());

        consumerMetrics.computeIfAbsent("0",
                consumerId -> new ConsumerMetrics(metricsCollector, consumerId)).update(stat);
    }

    public void update(ParallelPrepareStat stat) {
        this.droppedEvents.mark(stat.getDroppedEvents());
        this.filteredEvents.mark(stat.getFilteredEvents());
        this.totalEvents.mark(stat.getTotalEvents());

        this.deserializationTimeMs.update(stat.getDeserializationTimeMs());
        this.filtrationTimeMs.update(stat.getFiltrationTimeMs());
        this.processTimeMs.update(stat.getProcessTimeMs());
        this.filtrationTimePerEventNs.update(stat.getFiltrationTimePerEventNs());
        this.processTimePerEventNs.update(stat.getProcessTimePerEventNs());
    }

    /**
     * @return inner {@link MetricsCollector}
     */
    public IMetricsCollector getMetricsCollector() {
        return metricsCollector;
    }

    private String metricPath(String metricName) {
        return MetricsUtil.toMetricPath(SINK_METRICS_SCOPE, metricName);
    }

    public void updateBatch(int eventCount, long eventByteSize, int partitionCount) {
        batchEvents.update(eventCount);
        batchByteSize.update(eventByteSize);

        batchPartitionsCount.computeIfAbsent(partitionCount, value ->
                metricsCollector.meter(MetricsUtil.toMetricPath(SINK_METRICS_SCOPE, "batchPartitions", value.toString()))).mark();
    }

    public void update(ParallelSendStat stat) {
        this.sendingBatchesCount.update(stat.getSendingBatches());

        processSendTimeMs.update(stat.getProcessSendTimeMs());
    }

    public void update(ProcessorResult processorResult) {
        this.processedEvents.mark(processorResult.getProcessedEvents());
        this.rejectedEvents.mark(processorResult.getRejectedEvents());

        this.totalProcessAttempts.mark();
        if (processorResult.isSuccess()) {
            this.successProcessAttempts.mark();
        }
    }

    /**
     * Stores and sends {@link ru.kontur.vostok.hercules.sink.Sink Sink} metrics by Kafka-consumers.
     */
    private static class ConsumerMetrics {
        private final String consumerMetricsScope;

        private final IMetricsCollector metricsCollector;

        private final Timer pollTimeMs;
        private final Timer processTimeMs;
        private final Meter totalEvents;

        private final Map<TopicPartition, Meter> totalEventsPerPartition = new HashMap<>();
        private final Map<TopicPartition, Meter> batchSizePerPartition = new HashMap<>();

        private ConsumerMetrics(@NotNull IMetricsCollector metricsCollector, String consumerId) {
            this.consumerMetricsScope = MetricsUtil.toMetricPath(SINK_METRICS_SCOPE, ConsumerMetrics.class.getSimpleName(), consumerId);
            this.metricsCollector = metricsCollector;
            this.pollTimeMs = metricsCollector.timer(metricPath("pollTimeMs"));
            this.processTimeMs = metricsCollector.timer(metricPath("processTimeMs"));
            this.totalEvents = metricsCollector.meter(metricPath("totalEvents"));
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

        /**
         * Updates metric values by provided {@link Stat}.
         *
         * @param stat {@link Stat} contains calculated metric values
         */
        private void update(@NotNull KafkaEventSupplierStat stat) {
            this.pollTimeMs.update(stat.getPollTimeMs());

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

        private void clearUnsubscribedPartitionsMetrics(CurrentPartitionAssignmentSupplier stat, Map<TopicPartition, Meter> metrics) {
            Set<TopicPartition> currentPartitionAssignment = stat.getCurrentPartitionAssignment();
            Iterator<Map.Entry<TopicPartition, Meter>> iterator = metrics.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<TopicPartition, Meter> entry = iterator.next();
                if (!currentPartitionAssignment.contains(entry.getKey())) {
                    metricsCollector.remove(entry.getValue().name());
                    iterator.remove();
                }
            }
        }
    }

    public interface CurrentPartitionAssignmentSupplier {
        Set<TopicPartition> getCurrentPartitionAssignment();
    }
}
