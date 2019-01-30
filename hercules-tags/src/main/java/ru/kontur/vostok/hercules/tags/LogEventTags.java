package ru.kontur.vostok.hercules.tags;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.util.TagDescription;
import ru.kontur.vostok.hercules.protocol.util.TagDescriptionBuilder;

import javax.swing.text.html.Option;
import java.util.Optional;

/**
 * LogEventTags collection of tags of LogEvent
 *
 * @author Kirill Sulim
 */
public final class LogEventTags {

    /**
     * UTC offset in 100-ns ticks
     */
    public static TagDescription<Optional<Long>> UTC_OFFSET_TAG = TagDescriptionBuilder.longTag("utcOffset")
        .optional()
        .build();

    /**
     * Rendered log message
     */
    public static TagDescription<Optional<String>> MESSAGE_TAG = TagDescriptionBuilder.string("message")
        .optional()
        .build();

    /**
     * Message template
     */
    public static TagDescription<Optional<String>> MESSAGE_TEMPLATE_TAG = TagDescriptionBuilder.string("messageTemplate")
        .optional()
        .build();

    /**
     * Level
     */
    public static final TagDescription<Optional<String>> LEVEL_TAG = TagDescriptionBuilder.string("level")
        .optional()
        .build();

    /**
     * Exception
     */
    public static TagDescription<Optional<Container>> EXCEPTION_TAG = TagDescriptionBuilder.container("exception")
        .optional()
        .build();

    private LogEventTags() {
        /* static class */
    }
}
