package ru.kontur.vostok.hercules.graphite;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.tags.MetricsTags;

import java.util.Optional;

public class MetricEventFilter {
    public static boolean isValid(Event event) {
        if (event == null)
            return false;

        // Reject events with non-null aggregation type:
        if (ContainerUtil.extract(event.getPayload(), MetricsTags.AGGREGATION_TYPE_TAG).isPresent())
            return false;

        // Reject events without numeric metric value:
        if (!ContainerUtil.extract(event.getPayload(), MetricsTags.METRIC_VALUE_TAG).isPresent())
            return false;

        // Reject events without any tags:
        Optional<Container[]> tagsVector = ContainerUtil.extract(event.getPayload(), MetricsTags.TAGS_VECTOR_TAG);
        if (!tagsVector.isPresent() || tagsVector.get().length == 0)
            return false;

        return true;
    }
}
