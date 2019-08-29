package ru.kontur.vostok.hercules.kafka.util.metrics;

import org.apache.kafka.common.MetricName;
import ru.kontur.vostok.hercules.health.MetricsUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Grigory Ovchinnikov
 */
class KafkaMetricsUtil {
    /**
     * Build metric name in Graphite format from metric name in Apache Kafka format
     *
     * @param name is the metric name in Apache Kafka format
     * @return metric name in Graphite format
     */
    public static String toGraphiteMetricName(MetricName name) {
        List<String> tokens = new ArrayList<>();

        tokens.add("kafka");
        tokens.add(name.group());
        for (Map.Entry<String, String> tag : name.tags().entrySet()) {
            if (tag.getKey().length() <= 0 || tag.getValue().length() <= 0)
            {
                continue;
            }

            tokens.add(tag.getKey());
            tokens.add(tag.getValue());
        }
        tokens.add(name.name());

        return MetricsUtil.toMetricPath(tokens);
    }
}