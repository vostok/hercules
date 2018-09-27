package ru.kontur.vostok.hercules.sentry.sink.converters;

import io.sentry.event.interfaces.SentryException;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.tags.StackTraceTags;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;

/**
 * SentryExceptionConverter
 *
 * @author Kirill Sulim
 */
public class SentryExceptionConverter {

    public static SentryException convert(Container container) {
        String type = ContainerUtil.extract(container, StackTraceTags.TYPE_TAG);
        String value = ContainerUtil.extract(container, StackTraceTags.VALUE_TAG);
        String module = ContainerUtil.extract(container, StackTraceTags.EXCEPTION_MODULE_TAG);
        Container[] stacktrace = ContainerUtil.extract(container, StackTraceTags.STACKTRACE_TAG).orElseGet(() -> new Container[0]);

        return new SentryException(
                value,
                type,
                module,
                SentryStackTraceInterfaceConverter.convert(stacktrace)
        );
    }
}
