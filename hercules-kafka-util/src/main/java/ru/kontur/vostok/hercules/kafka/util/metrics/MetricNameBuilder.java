package ru.kontur.vostok.hercules.kafka.util.metrics;

import org.apache.kafka.common.MetricName;
import ru.kontur.vostok.hercules.health.MetricsUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MetricNameBuilder {
    public static String build(MetricName name) {
        List<String> tokens = new ArrayList<>();

        tokens.add("kafka");
        tokens.add(name.group());
        for (Map.Entry<String, String> tag : name.tags().entrySet()) {
            tokens.add(tag.getValue());
        }
        tokens.add(name.name());

        return MetricsUtil.toMetricNameFromTokens(tokens);
    }
}