package ru.kontur.vostok.hercules.sentry.sink.converters;

import io.sentry.event.interfaces.SentryException;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.tags.StackTraceTag;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;

/**
 * SentryExceptionConverter
 *
 * @author Kirill Sulim
 */
public class SentryExceptionConverter {

    public static SentryException convert(Container container) {
        String type = ContainerUtil.extractRequired(container, StackTraceTag.TYPE_TAG);
        String value = ContainerUtil.extractRequired(container, StackTraceTag.VALUE_TAG);
        String module = ContainerUtil.extractRequired(container, StackTraceTag.EXCEPTION_MODULE_TAG);
        Container[] stacktrace = ContainerUtil.extractRequired(container, StackTraceTag.STACKTRACE_TAG);

        return new SentryException(
                value,
                type,
                module,
                SentryStackTraceInterfaceConverter.convert(stacktrace)
        );
    }
}
