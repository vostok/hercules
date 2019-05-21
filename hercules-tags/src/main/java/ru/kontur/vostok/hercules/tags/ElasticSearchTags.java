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
    public static final TagDescription<Optional<String>> ELK_INDEX_TAG = TagDescriptionBuilder.string("elk-index")
        .optional()
        .build();

    /**
     * Uses for backward compatibility only. FIXME: Should be removed in upcoming releases.
     */
    @Deprecated
    public static final TagDescription<Optional<String>> ELK_SCOPE_TAG = TagDescriptionBuilder.string("elk-scope")
        .optional()
        .build();
}
