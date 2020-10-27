package ru.kontur.vostok.hercules.graphite.sink.converter;

import ru.kontur.vostok.hercules.graphite.sink.GraphiteMetricData;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.tags.MetricsTags;
import ru.kontur.vostok.hercules.util.metrics.GraphiteMetricsUtil;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

/**
 * Metric with tags event converter is used to convert Hercules event to Graphite metric data.
 * Metric name will be composed of tag which key value is "_name"
 * and set of other tags in format: "key=value", ​​separated by ";" symbol.
 *
 * @author Vladimir Tsypaev
 */
public class MetricWithTagsEventConverter implements MetricConverter {

    @Override
    public GraphiteMetricData convert(Event event) {
        String metricName = "unknown";
        StringBuilder tags = new StringBuilder();

        Container[] tagsVector = ContainerUtil.extract(event.getPayload(), MetricsTags.TAGS_VECTOR_TAG).get();
        for (Container tag : tagsVector) {
            String key = GraphiteMetricsUtil.sanitizeMetricName(
                    ContainerUtil.extract(tag, MetricsTags.TAG_KEY_TAG).orElse("null"));
            String value = GraphiteMetricsUtil.sanitizeMetricName(
                    ContainerUtil.extract(tag, MetricsTags.TAG_VALUE_TAG).orElse("null"));

            if (key.equals("_name")) {
                metricName = value;
                continue;
            }

            tags.append(";").append(key).append("=").append(value);
        }

        //TODO delete when found a better way to work with optional tags in Grafana
        String strTags = tags.toString();
        if (!strTags.contains(";subproject=") && strTags.contains(";project=")) {
            tags.append(";subproject=default");
        }

        String name = metricName + tags;
        long timestamp = TimeUtil.unixTicksToUnixTime(event.getTimestamp());
        double value = ContainerUtil.extract(event.getPayload(), MetricsTags.METRIC_VALUE_TAG).get();
        return new GraphiteMetricData(name, timestamp, value);
    }
}
