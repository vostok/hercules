package ru.kontur.vostok.hercules.tags;

import ru.kontur.vostok.hercules.protocol.util.TagDescription;
import ru.kontur.vostok.hercules.protocol.util.TagDescriptionBuilder;

import java.util.Optional;

public class UserTags {

    public static TagDescription<Optional<String>> ID_TAG = TagDescriptionBuilder.string("id")
            .optional()
            .build();

    public static TagDescription<Optional<String>> USERNAME_TAG = TagDescriptionBuilder.string("username")
            .optional()
            .build();

    public static TagDescription<Optional<String>> IP_ADDRESS_TAG = TagDescriptionBuilder.string("ipAddress")
            .optional()
            .build();

    public static TagDescription<Optional<String>> EMAIL_TAG = TagDescriptionBuilder.string("email")
            .optional()
            .build();
}
