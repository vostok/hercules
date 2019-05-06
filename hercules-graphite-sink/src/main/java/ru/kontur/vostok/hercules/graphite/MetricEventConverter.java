package ru.kontur.vostok.hercules.graphite;

import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.tags.MetricsTags;
import ru.kontur.vostok.hercules.util.metrics.GraphiteMetricsUtil;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MetricEventConverter {
    public static GraphiteMetricData convert(Event event) {
        String name = Stream.of(ContainerUtil.extract(event.getPayload(), MetricsTags.TAGS_VECTOR_TAG).get())
                .map(tag -> ContainerUtil.extract(tag, MetricsTags.TAG_VALUE_TAG).get())
                .map(GraphiteMetricsUtil::sanitizeMetricName)
                .collect(Collectors.joining("."));

        long timestamp = TimeUtil.unixTicksToUnixTime(event.getTimestamp());

        double value = ContainerUtil.extract(event.getPayload(), MetricsTags.METRIC_VALUE_TAG).get();

        return new GraphiteMetricData(name, timestamp, value);
    }
}

