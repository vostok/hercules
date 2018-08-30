package ru.kontur.vostok.hercules.tags;

import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.util.TagDescription;

/**
 * ElasticSearchTags collection of tags for elasticsearch related data
 *
 * @author Kirill Sulim
 */
public final class ElasticSearchTags {

    /**
     * Special name for marking use of special index in elasticsearch
     */
    public static final TagDescription INDEX_TAG = TagDescription.create("$index", Type.STRING);
}
