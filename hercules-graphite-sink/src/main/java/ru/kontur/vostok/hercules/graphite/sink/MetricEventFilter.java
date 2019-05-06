package ru.kontur.vostok.hercules.graphite.sink;

import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.tags.MetricsTags;

import java.util.Optional;

public class MetricEventFilter {
    public static boolean isValid(Event event) {
        if (ContainerUtil.extract(event.getPayload(), MetricsTags.AGGREGATION_TYPE_TAG).isPresent())
            return false;

        if (!ContainerUtil.extract(event.getPayload(), MetricsTags.METRIC_VALUE_TAG).isPresent())
            return false;

        if (!ContainerUtil.extract(event.getPayload(), MetricsTags.TAGS_VECTOR_TAG).isPresent())
            return false;

        return true;
    }
}
