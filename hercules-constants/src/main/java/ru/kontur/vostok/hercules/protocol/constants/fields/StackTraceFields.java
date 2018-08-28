package ru.kontur.vostok.hercules.protocol.constants.fields;

import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.util.FieldDescription;

/**
 * StackTraceFields collection of field for stacktrace info
 *
 * @author Kirill Sulim
 */
public final class StackTraceFields {

    // Root tags
    /**
     * List of exceptions from trown to causes
     */
    public static final FieldDescription EXCEPTIONS_FIELD = FieldDescription.create("exc", Type.CONTAINER_VECTOR);

    /**
     * Exception message
     */
    public static final FieldDescription MESSAGE_FIELD = FieldDescription.create("msg", Type.TEXT);

    /**
     * Error level
     */
    public static final FieldDescription LEVEL_FIELD = FieldDescription.create("lvl", Type.STRING);

    /**
     * Release name
     */
    public static final FieldDescription RELEASE_FIELD = FieldDescription.create("rlz", Type.STRING);

    /**
     * Server name
     */
    public static final FieldDescription SERVER_FIELD = FieldDescription.create("srv", Type.STRING);

    // Exception tags
    /**
     * Exception class simple name
     */
    public static final FieldDescription TYPE_FIELD = FieldDescription.create( "tp", Type.STRING);

    /**
     * Exception message
     */
    public static final FieldDescription VALUE_FIELD = FieldDescription.create("msg", Type.TEXT);

    /**
     * Exception module full name
     */
    public static final FieldDescription EXCEPTION_MODULE_FIELD = FieldDescription.create("mod", Type.TEXT);

    /**
     * Collection of stacktrace frame
     */
    public static final FieldDescription STACKTRACE_FIELD = FieldDescription.create("str", Type.CONTAINER_ARRAY);

    // Stack frame tags
    /**
     * Stacktrace frame module full name
     */
    public static final FieldDescription STACK_FRAME_MODULE_FIELD = FieldDescription.create("mod", Type.TEXT);

    /**
     * Stacktrace frame function name
     */
    public static final FieldDescription FUNCTION_FIELD = FieldDescription.create("fun", Type.STRING);

    /**
     * Stacktrace frame file name
     */
    public static final FieldDescription FILENAME_FIELD = FieldDescription.create("fnm", Type.STRING);

    /**
     * Stacktrace frame line number
     */
    public static final FieldDescription LINENO_FIELD =FieldDescription.create( "ln", Type.INTEGER);

    /**
     * Stacktrace frame column number
     */
    public static final FieldDescription COLNO_FIELD =FieldDescription.create( "cn", Type.SHORT);

    /**
     * Stacktrace frame file path
     */
    public static final FieldDescription ABS_PATH_FIELD = FieldDescription.create("abs", Type.TEXT);
}
