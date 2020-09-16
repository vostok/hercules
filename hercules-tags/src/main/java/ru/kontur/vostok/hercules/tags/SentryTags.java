package ru.kontur.vostok.hercules.tags;

import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.util.StandardExtractors;
import ru.kontur.vostok.hercules.protocol.util.TagDescription;
import ru.kontur.vostok.hercules.protocol.util.TagDescriptionBuilder;

import java.util.Optional;

/**
 * Tags for Sentry attributes and Sentry interfaces
 *
 * @author Petr Demenev
 */
public class SentryTags {

    /**
     * The release version of the application which generated event
     */
    public static TagDescription<Optional<String>> RELEASE_TAG = TagDescriptionBuilder.string("release")
            .optional()
            .build();

    /**
     * Trace identifier of the event
     */
    public static final TagDescription<Optional<String>> TRACE_ID_TAG = TagDescriptionBuilder.string("traceId")
            .addScalarExtractor(Type.UUID, Object::toString)
            .optional()
            .build();

    /**
     * The strings which are used for grouping the events by the issues in Sentry
     */
    public static final TagDescription<Optional<String[]>> FINGERPRINT_TAG = TagDescriptionBuilder.stringVector("fingerprint")
            .addScalarExtractor(Type.STRING, o -> new String[]{StandardExtractors.extractString(o)})
            .optional()
            .build();

    /**
     * The platform of the application which generated the event
     */
    public static TagDescription<Optional<String>> PLATFORM_TAG = TagDescriptionBuilder.string("platform")
            .optional()
            .build();

    /**
     * The name of the logger which created the event
     */
    public static TagDescription<Optional<String>> LOGGER_TAG = TagDescriptionBuilder.string("logger")
            .optional()
            .build();
}
