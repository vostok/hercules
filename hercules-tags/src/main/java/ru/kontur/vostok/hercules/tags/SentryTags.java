package ru.kontur.vostok.hercules.tags;

import ru.kontur.vostok.hercules.protocol.util.TagDescription;
import ru.kontur.vostok.hercules.protocol.util.TagDescriptionBuilder;

import java.util.Optional;

/**
 * Tags for Sentry attributes
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
     * The name of the transaction which caused this exception
     */
    public static final TagDescription<Optional<String>> TRANSACTION_TAG = TagDescriptionBuilder.string("transaction")
            .optional()
            .build();


    /**
     * Strings which are used for grouping events by issues in Sentry
     */
    public static final TagDescription<Optional<String[]>> FINGERPRINT_TAG = TagDescriptionBuilder.stringVector("fingerprint")
            .optional()
            .build();

    /**
     * Platform of the application which generated event
     */
    public static TagDescription<Optional<String>> PLATFORM_TAG = TagDescriptionBuilder.string("platform")
            .optional()
            .build();
}
