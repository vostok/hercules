package ru.kontur.vostok.hercules.tags;

import ru.kontur.vostok.hercules.protocol.util.TagDescription;
import ru.kontur.vostok.hercules.protocol.util.TagDescriptionBuilder;

import java.util.Optional;

/**
 * User tags for Sentry
 *
 * @author Petr Demenev
 */
public class UserTags {

    /**
     * The unique ID of the user
     */
    public static TagDescription<Optional<String>> ID_TAG = TagDescriptionBuilder.string("id")
            .optional()
            .build();

    /**
     * The username of the user
     */
    public static TagDescription<Optional<String>> USERNAME_TAG = TagDescriptionBuilder.string("username")
            .optional()
            .build();

    /**
     * The IP of the user
     */
    public static TagDescription<Optional<String>> IP_ADDRESS_TAG = TagDescriptionBuilder.string("ipAddress")
            .optional()
            .build();

    /**
     * The email address of the user
     */
    public static TagDescription<Optional<String>> EMAIL_TAG = TagDescriptionBuilder.string("email")
            .optional()
            .build();
}
