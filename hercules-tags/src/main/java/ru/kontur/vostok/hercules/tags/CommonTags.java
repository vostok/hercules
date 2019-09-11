package ru.kontur.vostok.hercules.tags;

import ru.kontur.vostok.hercules.protocol.Container;
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
     * Properties tag
     */
    public static final TagDescription<Optional<Container>> PROPERTIES_TAG = TagDescriptionBuilder.container("properties")
            .optional()
            .build();

    /**
     * Project tag
     */
    public static final TagDescription<Optional<String>> PROJECT_TAG = TagDescriptionBuilder.string("project")
            .optional()
            .build();

    /**
     * Subproject tag
     */
    public static final TagDescription<Optional<String>> SUBPROJECT_TAG = TagDescriptionBuilder.string("subproject")
            .optional()
            .build();

    /**
     * Application tag
     */
    public static final TagDescription<Optional<String>> APPLICATION_TAG = TagDescriptionBuilder.string("application")
            .optional()
            .build();

    /**
     * Service tag
     */
    public static final TagDescription<Optional<String>> SERVICE_TAG = TagDescriptionBuilder.string("service")
            .optional()
            .build();

    /**
     * Environment tag
     */
    public static final TagDescription<Optional<String>> ENVIRONMENT_TAG = TagDescriptionBuilder.string("environment")
            .optional()
            .build();

    private CommonTags() {
        /* static class */
    }
}
