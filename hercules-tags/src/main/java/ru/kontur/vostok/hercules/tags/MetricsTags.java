package ru.kontur.vostok.hercules.tags;

import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.util.TagDescription;

/**
 * MetricsTags collection of metrics tags
 *
 * @author Kirill Sulim
 */
public final class MetricsTags {

    /**
     * Metric name tag
     */
    public static final TagDescription METRIC_NAME_TAG = TagDescription.create("metric-name", Type.TEXT);

    /**
     * Metric value tag
     */
    public static final TagDescription METRIC_VALUE_TAG = TagDescription.create("metric-value", Type.DOUBLE);
}
