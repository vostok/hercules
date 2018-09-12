package ru.kontur.vostok.hercules.sentry.sink.converters;

import io.sentry.event.interfaces.SentryStackTraceElement;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.tags.StackTraceTags;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;

/**
 * SentryStackTraceElementConverter
 *
 * @author Kirill Sulim
 */
public class SentryStackTraceElementConverter {

    public static SentryStackTraceElement convert(Container container) {
        return new SentryStackTraceElement(
                ContainerUtil.extract(container, StackTraceTags.STACK_FRAME_MODULE_TAG),
                ContainerUtil.extract(container, StackTraceTags.FUNCTION_TAG),
                ContainerUtil.extract(container, StackTraceTags.FILENAME_TAG).orElse(null),
                ContainerUtil.extract(container, StackTraceTags.LINE_NUMBER_TAG).orElse(0),
                ContainerUtil.extract(container, StackTraceTags.COLUMN_NUMBER_TAG).orElse(null),
                ContainerUtil.extract(container, StackTraceTags.ABS_PATH_TAG).orElse(null),
                null
        );
    }
}
