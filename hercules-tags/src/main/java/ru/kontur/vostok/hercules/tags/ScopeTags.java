package ru.kontur.vostok.hercules.tags;

import ru.kontur.vostok.hercules.protocol.util.TagDescription;
import ru.kontur.vostok.hercules.protocol.util.TagDescriptionBuilder;

import java.util.Optional;

public class ScopeTags {

    /**
     * Special tag for marking mid-level hierarchy in elasticsearch index and for marking Sentry project
     */
    public static final TagDescription<Optional<String>> SCOPE_TAG = TagDescriptionBuilder.string("scope")
            .optional()
            .build();
}
