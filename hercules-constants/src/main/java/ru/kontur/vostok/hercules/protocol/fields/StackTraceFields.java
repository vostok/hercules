package ru.kontur.vostok.hercules.protocol.fields;

import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.util.FieldDescription;

/**
 * StackTraceFields collection of field for stacktrace info
 *
 * @author Kirill Sulim
 */
public class StackTraceFields {

    // Root tags
    public static final FieldDescription EXCEPTIONS_FIELD = FieldDescription.create("exc", Type.CONTAINER_VECTOR);
    public static final FieldDescription MESSAGE_FIELD = FieldDescription.create("msg", Type.TEXT);
    public static final FieldDescription LEVEL_FIELD = FieldDescription.create("lvl", Type.STRING);
    public static final FieldDescription RELEASE_FIELD = FieldDescription.create("rlz", Type.STRING);
    public static final FieldDescription SERVER_FIELD = FieldDescription.create("srv", Type.STRING);

    // Exception tags
    public static final FieldDescription TYPE_FIELD = FieldDescription.create( "tp", Type.STRING);
    public static final FieldDescription VALUE_FIELD = FieldDescription.create("msg", Type.TEXT);
    public static final FieldDescription EXCEPTION_MODULE_FIELD = FieldDescription.create("mod", Type.TEXT);
    public static final FieldDescription STACKTRACE_FIELD = FieldDescription.create("str", Type.CONTAINER_ARRAY);

    // Stack frame tags
    public static final FieldDescription STACK_FRAME_MODULE_FIELD = FieldDescription.create("mod", Type.TEXT);
    public static final FieldDescription FUNCTION_FIELD = FieldDescription.create("fun", Type.STRING);
    public static final FieldDescription FILENAME_FIELD = FieldDescription.create("fnm", Type.STRING);
    public static final FieldDescription LINENO_FIELD =FieldDescription.create( "ln", Type.INTEGER);
    public static final FieldDescription COLNO_FIELD =FieldDescription.create( "cn", Type.SHORT);
    public static final FieldDescription ABS_PATH_FIELD = FieldDescription.create("abs", Type.TEXT);
}
