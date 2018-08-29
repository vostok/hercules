package ru.kontur.vostok.hercules.sentry.sink.converters;

import io.sentry.event.interfaces.SentryStackTraceElement;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.tags.StackTraceTag;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;

/**
 * SentryStackTraceElementConverter
 *
 * @author Kirill Sulim
 */
public class SentryStackTraceElementConverter {

    public static SentryStackTraceElement convert(Container container) {
        return new SentryStackTraceElement(
                ContainerUtil.extractRequired(container, StackTraceTag.STACK_FRAME_MODULE_TAG),
                ContainerUtil.extractRequired(container, StackTraceTag.FUNCTION_TAG),
                ContainerUtil.extractRequired(container, StackTraceTag.FILENAME_TAG),
                ContainerUtil.extractRequired(container, StackTraceTag.LINE_NUMBER_TAG),
                ContainerUtil.<Short>extractOptional(container, StackTraceTag.COLUMN_NUMBER_TAG).map(Short::intValue).orElse(null),
                ContainerUtil.extractRequired(container, StackTraceTag.ABS_PATH_TAG),
                null
        );
    }
}
