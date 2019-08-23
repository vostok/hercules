package ru.kontur.vostok.hercules.kafka.util.metrics;

import org.apache.kafka.common.MetricName;
import ru.kontur.vostok.hercules.util.metrics.GraphiteMetricsUtil;

import java.util.*;

public class MetricNameBuilder {
    public static String build(MetricName name) {
        List<String> tokens = new ArrayList<>();

        tokens.add("kafka");
        tokens.add(name.group());
        for (Map.Entry<String, String> tag : name.tags().entrySet()) {
            tokens.add(tag.getValue());
        }
        tokens.add(name.name());

        return GraphiteMetricsUtil.buildMetricName(tokens);
    }
}