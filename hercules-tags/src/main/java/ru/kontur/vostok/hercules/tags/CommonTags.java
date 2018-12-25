package ru.kontur.vostok.hercules.tags;

import ru.kontur.vostok.hercules.protocol.util.TagDescription;
import ru.kontur.vostok.hercules.protocol.util.TagDescriptionBuilder;

import java.util.Optional;

/**
 * CommonTags collection of common tags
 *
 * @author Kirill Sulim
 */
public final class CommonTags {

    /**
     * Environment tag
     */
    public static final TagDescription<Optional<String>> ENVIRONMENT_TAG = TagDescriptionBuilder.string("env")
            .convert(Optional::of)
            .addDefault(Optional::empty)
            .build();

    /**
     * Project tag
     */
    public static final TagDescription<Optional<String>> PROJECT_TAG = TagDescriptionBuilder.string("proj")
            .convert(Optional::of)
            .addDefault(Optional::empty)
            .build();

    private CommonTags() {
    }
}
