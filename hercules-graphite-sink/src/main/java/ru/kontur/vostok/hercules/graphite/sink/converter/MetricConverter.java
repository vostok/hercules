package ru.kontur.vostok.hercules.graphite.sink.converter;

import ru.kontur.vostok.hercules.graphite.sink.GraphiteMetricData;
import ru.kontur.vostok.hercules.protocol.Event;

/**
 * Metric converter is used to convert Hercules event to Graphite metric data.
 *
 * @author Vladimir Tsypaev
 */
public interface MetricConverter {
    GraphiteMetricData convert(Event event);
}
