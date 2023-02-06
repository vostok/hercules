package ru.kontur.vostok.hercules.tags;

import java.util.Optional;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.util.StandardExtractors;
import ru.kontur.vostok.hercules.protocol.util.TagDescription;
import ru.kontur.vostok.hercules.protocol.util.TagDescriptionBuilder;

/**
 * MetricsTags collection of metrics tags
 *
 * @author iloktionov
 */
public final class MetricsTags {

    public static final TagDescription<Optional<Container[]>> TAGS_VECTOR_TAG = TagDescriptionBuilder.containerVector("tags")
            .optional()
            .build();

    public static final TagDescription<Optional<String>> TAG_KEY_TAG = TagDescriptionBuilder.string("key")
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

    public static final TagDescription<Optional<Container>> ENRICHMENT_TAG = TagDescriptionBuilder.container("enrichment")
            .optional()
            .build();

    public static final TagDescription<Optional<String>> TOPIC_NAME_TAG = TagDescriptionBuilder
            .string("topic")
            .optional()
            .build();

    public static final TagDescription<Optional<String>> NAME_PATTERN_TAG = TagDescriptionBuilder
            .string("name_pattern")
            .optional()
            .build();

    public static final TagDescription<Optional<String>> NEW_NAME_TAG = TagDescriptionBuilder
            .string("new_name")
            .optional()
            .build();
}
