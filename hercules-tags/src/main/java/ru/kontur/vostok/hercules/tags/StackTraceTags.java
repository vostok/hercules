package ru.kontur.vostok.hercules.tags;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.util.TagDescription;
import ru.kontur.vostok.hercules.protocol.util.TagDescriptionBuilder;

import java.util.Optional;

/**
 * StackTraceTags collection of tag for stacktrace info
 *
 * @author Kirill Sulim
 * @deprecated Schema was changed, use new schema
 */
@Deprecated
public final class StackTraceTags {

    // Root tags
    /**
     * List of exceptions from trown to causes
     */
    public static final TagDescription<Optional<Container[]>> EXCEPTIONS_TAG = TagDescriptionBuilder
            .containerVector("exc")
            .convert(Optional::of)
            .addDefault(Optional::empty)
            .build();

    /**
     * Exception message
     */
    public static final TagDescription<Optional<String>> MESSAGE_TAG = TagDescriptionBuilder.string("msg")
            .convert(Optional::of)
            .addDefault(Optional::empty)
            .build();

    /**
     * Error level
     */
    public static final TagDescription<Optional<String>> LEVEL_TAG = TagDescriptionBuilder.string("lvl")
            .convert(Optional::of)
            .addDefault(Optional::empty)
            .build();

    /**
     * Release name
     */
    public static final TagDescription<Optional<String>> RELEASE_TAG = TagDescriptionBuilder.string("rlz")
            .convert(Optional::of)
            .addDefault(Optional::empty)
            .build();

    /**
     * Server name
     */
    public static final TagDescription<Optional<String>> SERVER_TAG = TagDescriptionBuilder.string("srv")
            .convert(Optional::of)
            .addDefault(Optional::empty)
            .build();

    // Exception tags
    /**
     * Exception class simple name
     */
    public static final TagDescription<String> TYPE_TAG = TagDescriptionBuilder.string("tp").build();

    /**
     * Exception message
     */
    public static final TagDescription<String> VALUE_TAG = TagDescriptionBuilder.string("msg").build();

    /**
     * Exception module full name
     */
    public static final TagDescription<String> EXCEPTION_MODULE_TAG = TagDescriptionBuilder.string("mod").build();

    /**
     * Collection of stacktrace frame
     */
    public static final TagDescription<Optional<Container[]>> STACKTRACE_TAG = TagDescriptionBuilder
            .containerVector("str")
            .convert(Optional::of)
            .addDefault(Optional::empty)
            .build();

    // Stack frame tags
    /**
     * Stacktrace frame module full name
     */
    public static final TagDescription<String> STACK_FRAME_MODULE_TAG = TagDescriptionBuilder.string("mod").build();

    /**
     * Stacktrace frame function name
     */
    public static final TagDescription<String> FUNCTION_TAG = TagDescriptionBuilder.string("fun").build();

    /**
     * Stacktrace frame file name
     */
    public static final TagDescription<Optional<String>> FILENAME_TAG = TagDescriptionBuilder.string("fnm")
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
    public static final TagDescription<Optional<String>> ABS_PATH_TAG = TagDescriptionBuilder.string("abs")
            .convert(Optional::of)
            .addDefault(Optional::empty)
            .build();
}
