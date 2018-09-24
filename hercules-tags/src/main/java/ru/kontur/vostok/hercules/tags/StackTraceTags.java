package ru.kontur.vostok.hercules.tags;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.util.TagDescription;
import ru.kontur.vostok.hercules.protocol.util.TagDescriptionBuilder;

import java.util.Optional;

/**
 * StackTraceTags collection of tag for stacktrace info
 *
 * @author Kirill Sulim
 */
public final class StackTraceTags {

    // Root tags
    /**
     * List of exceptions from trown to causes
     */
    public static final TagDescription<Optional<Container[]>> EXCEPTIONS_TAG = TagDescriptionBuilder
            .containerList("exc")
            .convert(Optional::of)
            .addDefault(Optional::empty)
            .build();

    /**
     * Exception message
     */
    public static final TagDescription<Optional<String>> MESSAGE_TAG = TagDescriptionBuilder.textual("msg")
            .convert(Optional::of)
            .addDefault(Optional::empty)
            .build();

    /**
     * Error level
     */
    public static final TagDescription<Optional<String>> LEVEL_TAG = TagDescriptionBuilder.textual("lvl")
            .convert(Optional::of)
            .addDefault(Optional::empty)
            .build();

    /**
     * Release name
     */
    public static final TagDescription<Optional<String>> RELEASE_TAG = TagDescriptionBuilder.textual("rlz")
            .convert(Optional::of)
            .addDefault(Optional::empty)
            .build();

    /**
     * Server name
     */
    public static final TagDescription<Optional<String>> SERVER_TAG = TagDescriptionBuilder.textual("srv")
            .convert(Optional::of)
            .addDefault(Optional::empty)
            .build();

    // Exception tags
    /**
     * Exception class simple name
     */
    public static final TagDescription<String> TYPE_TAG = TagDescriptionBuilder.textual("tp").build();

    /**
     * Exception message
     */
    public static final TagDescription<String> VALUE_TAG = TagDescriptionBuilder.textual("msg").build();

    /**
     * Exception module full name
     */
    public static final TagDescription<String> EXCEPTION_MODULE_TAG = TagDescriptionBuilder.textual("mod").build();

    /**
     * Collection of stacktrace frame
     */
    public static final TagDescription<Optional<Container[]>> STACKTRACE_TAG = TagDescriptionBuilder
            .containerList("str")
            .convert(Optional::of)
            .addDefault(Optional::empty)
            .build();

    // Stack frame tags
    /**
     * Stacktrace frame module full name
     */
    public static final TagDescription<String> STACK_FRAME_MODULE_TAG = TagDescriptionBuilder.textual("mod").build();

    /**
     * Stacktrace frame function name
     */
    public static final TagDescription<String> FUNCTION_TAG = TagDescriptionBuilder.textual("fun").build();

    /**
     * Stacktrace frame file name
     */
    public static final TagDescription<Optional<String>> FILENAME_TAG = TagDescriptionBuilder.textual("fnm")
            .convert(Optional::of)
            .addDefault(Optional::empty)
            .build();

    /**
     * Stacktrace frame line number
     */
    public static final TagDescription<Optional<Integer>> LINE_NUMBER_TAG = TagDescriptionBuilder.integer("ln")
            .convert(Optional::of)
            .addDefault(Optional::empty)
            .build();

    /**
     * Stacktrace frame column number
     */
    public static final TagDescription<Optional<Integer>> COLUMN_NUMBER_TAG = TagDescriptionBuilder.integer("cn")
            .convert(Optional::ofNullable)
            .addDefault(Optional::empty)
            .build();

    /**
     * Stacktrace frame file path
     */
    public static final TagDescription<Optional<String>> ABS_PATH_TAG = TagDescriptionBuilder.textual("abs")
            .convert(Optional::of)
            .addDefault(Optional::empty)
            .build();
}
