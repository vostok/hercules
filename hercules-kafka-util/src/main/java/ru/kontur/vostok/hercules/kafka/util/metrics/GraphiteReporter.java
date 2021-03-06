package ru.kontur.vostok.hercules.kafka.util.metrics;

import org.apache.kafka.common.metrics.KafkaMetric;
import org.apache.kafka.common.metrics.MetricsReporter;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.kafka.util.KafkaConfigs;

import java.util.List;
import java.util.Map;

/**
 * Register Apache Kafka client metrics in Graphite using {@link MetricsCollector}.
 * {@link MetricsCollector} instance should be provided through `{@value KafkaConfigs#METRICS_COLLECTOR_INSTANCE_CONFIG}` property.
 *
 * @author Grigory Ovchinnikov
 */
public class GraphiteReporter implements MetricsReporter {
    private MetricsCollector metricsCollector;

    @Override
    public void init(List<KafkaMetric> list) {
        list.forEach(this::register);
    }

    @Override
    public void metricChange(KafkaMetric kafkaMetric) {
        register(kafkaMetric);
    }

    @Override
    public void metricRemoval(KafkaMetric kafkaMetric) {
        unregister(kafkaMetric);
    }

    @Override
    public void close() {
    }

    @Override
    public void configure(Map<String, ?> map) {
        if (!map.containsKey(KafkaConfigs.METRICS_COLLECTOR_INSTANCE_CONFIG)) {
            throw new IllegalArgumentException(String.format("There is no '%s' property",
                    KafkaConfigs.METRICS_COLLECTOR_INSTANCE_CONFIG));
        }

        metricsCollector = ((MetricsCollector) map.get(KafkaConfigs.METRICS_COLLECTOR_INSTANCE_CONFIG));
    }

    private void register(KafkaMetric kafkaMetric) {
        String metricName = KafkaMetricsUtil.toGraphiteMetricName(kafkaMetric.metricName());
        metricsCollector.gauge(metricName, kafkaMetric::metricValue);
    }

    private void unregister(KafkaMetric kafkaMetric) {
        String metricName = KafkaMetricsUtil.toGraphiteMetricName(kafkaMetric.metricName());
        metricsCollector.remove(metricName);
    }
}