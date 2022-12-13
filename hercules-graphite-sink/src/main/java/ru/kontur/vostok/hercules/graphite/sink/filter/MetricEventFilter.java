package ru.kontur.vostok.hercules.graphite.sink.filter;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.sink.filter.EventFilter;
import ru.kontur.vostok.hercules.tags.MetricsTags;

import java.util.Optional;
import java.util.Properties;

/**
 * Metric event filter uses for filtered out events:
 * 1. that contain a {@link MetricsTags#AGGREGATION_TYPE_TAG};
 * 2. with not numerical {@link MetricsTags#METRIC_VALUE_TAG} value;
 * 3. that contain empty {@link MetricsTags#TAGS_VECTOR_TAG} or not contain it.
 *
 * @author Vladimir Tsypaev
 */
public class MetricEventFilter extends EventFilter {

    public MetricEventFilter(Properties properties) {
        super(properties);
    }

    @Override
    public boolean test(Event event) {
        if (event.getPayload().tags().containsKey(MetricsTags.AGGREGATION_TYPE_TAG.getName())) {
            return false;
        }

        Optional<Double> metricValue = ContainerUtil.extract(event.getPayload(), MetricsTags.METRIC_VALUE_TAG);
        if (metricValue.isEmpty() || !Double.isFinite(metricValue.get())) {
            return false;
        }

        Optional<Container[]> tagsVector = ContainerUtil.extract(event.getPayload(), MetricsTags.TAGS_VECTOR_TAG);
        if (tagsVector.isEmpty() || tagsVector.get().length == 0) {
            return false;
        }

        return true;
    }
}
