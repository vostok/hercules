package ru.kontur.vostok.hercules.sentry.sink.converters;

import io.sentry.event.interfaces.SentryStackTraceElement;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.protocol.util.FieldDescription;

/**
 * SentryStackTraceElementConverter
 *
 * @author Kirill Sulim
 */
public class SentryStackTraceElementConverter {

    private static final FieldDescription MODULE_FIELD = FieldDescription.create("mod", Type.TEXT);
    private static final FieldDescription FUNCTION_FIELD = FieldDescription.create("fun", Type.STRING);
    public static final FieldDescription FILENAME_FIELD = FieldDescription.create("fnm", Type.STRING);
    private static final FieldDescription LINENO_FIELD =FieldDescription.create( "ln", Type.INTEGER);
    private static final FieldDescription COLNO_FIELD =FieldDescription.create( "cn", Type.SHORT);
    private static final FieldDescription ABS_PATH_FIELD = FieldDescription.create("abs", Type.TEXT);

    public static SentryStackTraceElement convert(Container container) {
        return new SentryStackTraceElement(
                ContainerUtil.extractRequired(container, MODULE_FIELD),
                ContainerUtil.extractRequired(container, FUNCTION_FIELD),
                ContainerUtil.extractRequired(container, FILENAME_FIELD),
                ContainerUtil.extractRequired(container, LINENO_FIELD),
                ContainerUtil.<Short>extractOptional(container, COLNO_FIELD).map(Short::intValue).orElse(null),
                ContainerUtil.extractRequired(container, ABS_PATH_FIELD),
                null
        );
    }
}
