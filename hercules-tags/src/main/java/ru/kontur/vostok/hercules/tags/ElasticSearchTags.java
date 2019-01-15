package ru.kontur.vostok.hercules.tags;

import ru.kontur.vostok.hercules.protocol.util.TagDescription;
import ru.kontur.vostok.hercules.protocol.util.TagDescriptionBuilder;

import java.util.Optional;

/**
 * ElasticSearchTags collection of tags for elasticsearch related data
 *
 * @author Kirill Sulim
 */
public final class ElasticSearchTags {

    /**
     * Special name for marking use of special index in elasticsearch
     */
    public static final TagDescription<Optional<String>> INDEX_PATTERN_TAG = TagDescriptionBuilder.string("elk-index")
            .optional()
            .build();
}
