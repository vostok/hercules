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
                ContainerUtil.extract(container, StackTraceTag.STACK_FRAME_MODULE_TAG),
                ContainerUtil.extract(container, StackTraceTag.FUNCTION_TAG),
                ContainerUtil.extract(container, StackTraceTag.FILENAME_TAG).orElse(null),
                ContainerUtil.extract(container, StackTraceTag.LINE_NUMBER_TAG).orElse(0),
                ContainerUtil.extract(container, StackTraceTag.COLUMN_NUMBER_TAG).orElse(null),
                ContainerUtil.extract(container, StackTraceTag.ABS_PATH_TAG).orElse(null),
                null
        );
    }
}
