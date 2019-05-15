package ru.kontur.vostok.hercules.tags;

import ru.kontur.vostok.hercules.protocol.Container;
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

    public static final TagDescription<Optional<Container[]>> TAGS_VECTOR_TAG = TagDescriptionBuilder.containerVector("tags")
            .optional()
            .build();

    public static final TagDescription<Optional<String>> TAG_VALUE_TAG = TagDescriptionBuilder.string("value")
            .optional()
            .build();

    public static final TagDescription<Optional<Double>> METRIC_VALUE_TAG = TagDescriptionBuilder.tag("value", Double.class)
            .addScalarExtractor(Type.DOUBLE, StandardExtractors::extractDouble)
            .optional()
            .build();

    public static final TagDescription<Optional<String>> AGGREGATION_TYPE_TAG = TagDescriptionBuilder.string("aggregationType")
            .optional()
            .build();
}
