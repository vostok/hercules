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
                ContainerUtil.<String>extractOptional(container, StackTraceTag.FILENAME_TAG).orElse(null),
                ContainerUtil.<Integer>extractOptional(container, StackTraceTag.LINE_NUMBER_TAG).orElse(0),
                ContainerUtil.<Integer>extractOptional(container, StackTraceTag.COLUMN_NUMBER_TAG).orElse(null),
                ContainerUtil.<String>extractOptional(container, StackTraceTag.ABS_PATH_TAG).orElse(null),
                null
        );
    }
}
