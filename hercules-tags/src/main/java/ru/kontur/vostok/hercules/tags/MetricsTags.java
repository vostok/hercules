package ru.kontur.vostok.hercules.tags;

import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.util.StandardExtractors;
import ru.kontur.vostok.hercules.protocol.util.TagDescription;
import ru.kontur.vostok.hercules.protocol.util.TagDescriptionBuilder;

import java.util.Optional;

/**
 * MetricsTags collection of metrics tags
 *
 * @author Kirill Sulim
 */
public final class MetricsTags {

    /**
     * Metric name tag
     */
    public static final TagDescription<Optional<String>> METRIC_NAME_TAG = TagDescriptionBuilder.textual("metric-name")
            .convert(Optional::of)
            .addDefault(Optional::empty)
            .build();

    /**
     * Metric value tag
     */
    public static final TagDescription<Optional<Double>> METRIC_VALUE_TAG = TagDescriptionBuilder.tag("metric-value", Double.class)
            .addExtractor(Type.DOUBLE, StandardExtractors::extractDouble)
            .convert(Optional::of)
            .addDefault(Optional::empty)
            .build();
}
