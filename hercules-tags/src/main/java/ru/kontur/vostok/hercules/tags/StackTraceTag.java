package ru.kontur.vostok.hercules.tags;

import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.util.TagDescription;

/**
 * StackTraceTag collection of tag for stacktrace info
 *
 * @author Kirill Sulim
 */
public final class StackTraceTag {

    // Root tags
    /**
     * List of exceptions from trown to causes
     */
    public static final TagDescription EXCEPTIONS_TAG = TagDescription.create("exc", Type.CONTAINER_VECTOR);

    /**
     * Exception message
     */
    public static final TagDescription MESSAGE_TAG = TagDescription.create("msg", Type.TEXT);

    /**
     * Error level
     */
    public static final TagDescription LEVEL_TAG = TagDescription.create("lvl", Type.STRING);

    /**
     * Release name
     */
    public static final TagDescription RELEASE_TAG = TagDescription.create("rlz", Type.STRING);

    /**
     * Server name
     */
    public static final TagDescription SERVER_TAG = TagDescription.create("srv", Type.STRING);

    // Exception tags
    /**
     * Exception class simple name
     */
    public static final TagDescription TYPE_TAG = TagDescription.create( "tp", Type.STRING);

    /**
     * Exception message
     */
    public static final TagDescription VALUE_TAG = TagDescription.create("msg", Type.TEXT);

    /**
     * Exception module full name
     */
    public static final TagDescription EXCEPTION_MODULE_TAG = TagDescription.create("mod", Type.TEXT);

    /**
     * Collection of stacktrace frame
     */
    public static final TagDescription STACKTRACE_TAG = TagDescription.create("str", Type.CONTAINER_ARRAY);

    // Stack frame tags
    /**
     * Stacktrace frame module full name
     */
    public static final TagDescription STACK_FRAME_MODULE_TAG = TagDescription.create("mod", Type.TEXT);

    /**
     * Stacktrace frame function name
     */
    public static final TagDescription FUNCTION_TAG = TagDescription.create("fun", Type.STRING);

    /**
     * Stacktrace frame file name
     */
    public static final TagDescription FILENAME_TAG = TagDescription.create("fnm", Type.STRING);

    /**
     * Stacktrace frame line number
     */
    public static final TagDescription LINE_NUMBER_TAG = TagDescription.create( "ln", Type.INTEGER);

    /**
     * Stacktrace frame column number
     */
    public static final TagDescription COLUMN_NUMBER_TAG = TagDescription.create( "cn", Type.INTEGER);

    /**
     * Stacktrace frame file path
     */
    public static final TagDescription ABS_PATH_TAG = TagDescription.create("abs", Type.TEXT);
}
