package ru.kontur.vostok.hercules.tags;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.util.TagDescription;
import ru.kontur.vostok.hercules.protocol.util.TagDescriptionBuilder;

import java.util.Optional;

/**
 * ExceptionTags
 *
 * @author Kirill Sulim
 */
public final class ExceptionTags {

    /**
     * Exception runtime type
     */
    public static final TagDescription<Optional<String>> TYPE_TAG = TagDescriptionBuilder.string("type")
        .optional()
        .build();

    /**
     * Exception message
     */
    public static final TagDescription<Optional<String>> MESSAGE_TAG = TagDescriptionBuilder.string("message")
        .optional()
        .build();

    /**
     * Inner exceptions
     */
    public static final TagDescription<Optional<Container[]>> INNER_EXCEPTIONS_TAG = TagDescriptionBuilder.containerVector("innerExceptions")
        .optional()
        .build();

    /**
     * Exception stack frames
     */
    public static final TagDescription<Optional<Container[]>> STACK_FRAMES = TagDescriptionBuilder.containerVector("stackFrames")
        .optional()
        .build();

    private ExceptionTags() {
        /* static class */
    }
}
