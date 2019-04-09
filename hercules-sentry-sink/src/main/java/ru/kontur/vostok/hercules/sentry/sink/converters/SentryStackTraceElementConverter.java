package ru.kontur.vostok.hercules.sentry.sink.converters;

import io.sentry.event.interfaces.SentryStackTraceElement;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.tags.StackFrameTags;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;

/**
 * SentryStackTraceElementConverter
 * Allows to convert exception stack frames from a Hercules event
 * to a Sentry stacktrace in a Sentry event
 *
 * @author Kirill Sulim
 */
public class SentryStackTraceElementConverter {

    /**
     * @param container the container with values of the StackFrame tags of a Hercules event
     * @return the Sentry stacktrace element
     */
    public static SentryStackTraceElement convert(final Container container) {

        final String file = ContainerUtil.extract(container, StackFrameTags.FILE_TAG).orElse(null);

        return new SentryStackTraceElement(
            ContainerUtil.extract(container, StackFrameTags.TYPE_TAG).orElse(null),
            ContainerUtil.extract(container, StackFrameTags.FUNCTION_TAG).orElse(null),
            file,
            ContainerUtil.extract(container, StackFrameTags.LINE_NUMBER_TAG).orElse(0),
            ContainerUtil.extract(container, StackFrameTags.COLUMN_NUMBER_TAG).map(s -> (int) s).orElse(null),
            file,
            null
        );
    }
}
