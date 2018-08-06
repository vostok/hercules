package ru.kontur.vostok.hercules.sentry.sink.converters;

import io.sentry.event.interfaces.SentryStackTraceElement;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;

/**
 * SentryStackTraceElementConverter
 *
 * @author Kirill Sulim
 */
public class SentryStackTraceElementConverter {

    private static final String MODULE_FIELD_NAME = "mod";
    private static final String FUNCTION_FIELD_NAME = "fun";
    public static final String FILENAME_FIELD_NAME = "fnm";
    private static final String LINENO_FIELD_NAME = "ln";
    private static final String COLNO_FIELD_NAME = "cn";
    private static final String ABS_PATH_FIELD_NAME = "abs";

    public static SentryStackTraceElement convert(Container container) {
        return new SentryStackTraceElement(
                ContainerUtil.extractRequired(container, MODULE_FIELD_NAME, Type.TEXT),
                ContainerUtil.extractRequired(container, FUNCTION_FIELD_NAME, Type.STRING),
                ContainerUtil.extractRequired(container, FILENAME_FIELD_NAME, Type.STRING),
                ContainerUtil.extractRequired(container, LINENO_FIELD_NAME, Type.INTEGER),
                ContainerUtil.<Short>extractOptional(container, COLNO_FIELD_NAME, Type.SHORT).map(Short::intValue).orElse(null),
                ContainerUtil.extractRequired(container, ABS_PATH_FIELD_NAME, Type.TEXT),
                null
        );
    }
}
