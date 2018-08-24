package ru.kontur.vostok.hercules.sentry.sink.converters;

import io.sentry.event.interfaces.SentryStackTraceElement;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.tags.StackTraceFields;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;

/**
 * SentryStackTraceElementConverter
 *
 * @author Kirill Sulim
 */
public class SentryStackTraceElementConverter {

    public static SentryStackTraceElement convert(Container container) {
        return new SentryStackTraceElement(
                ContainerUtil.extractRequired(container, StackTraceFields.STACK_FRAME_MODULE_FIELD),
                ContainerUtil.extractRequired(container, StackTraceFields.FUNCTION_FIELD),
                ContainerUtil.extractRequired(container, StackTraceFields.FILENAME_FIELD),
                ContainerUtil.extractRequired(container, StackTraceFields.LINENO_FIELD),
                ContainerUtil.<Short>extractOptional(container, StackTraceFields.COLNO_FIELD).map(Short::intValue).orElse(null),
                ContainerUtil.extractRequired(container, StackTraceFields.ABS_PATH_FIELD),
                null
        );
    }
}
