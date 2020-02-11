package ru.kontur.vostok.hercules.graphite.sink.converter;

import ru.kontur.vostok.hercules.graphite.sink.GraphiteMetricData;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.tags.MetricsTags;
import ru.kontur.vostok.hercules.util.metrics.GraphiteMetricsUtil;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Metric event converter is used to convert Hercules event to Graphite metric data.
 * Metric name will be composed of all tag values ​​separated by "." symbol.
 *
 * @author Vladimir Tsypaev
 */
public class MetricEventConverter implements MetricConverter {

    @Override
    public GraphiteMetricData convert(Event event) {
        String name = Stream.of(ContainerUtil.extract(event.getPayload(), MetricsTags.TAGS_VECTOR_TAG).get())
                .map(tag -> ContainerUtil.extract(tag, MetricsTags.TAG_VALUE_TAG).orElse("null"))
                .map(GraphiteMetricsUtil::sanitizeMetricName)
                .collect(Collectors.joining("."));

        long timestamp = TimeUtil.unixTicksToUnixTime(event.getTimestamp());
        double value = ContainerUtil.extract(event.getPayload(), MetricsTags.METRIC_VALUE_TAG).get();
        return new GraphiteMetricData(name, timestamp, value);
    }
}
