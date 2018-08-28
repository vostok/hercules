package ru.kontur.vostok.hercules.sentry.sink.converters;

import io.sentry.event.interfaces.SentryException;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.constants.fields.StackTraceFields;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;

/**
 * SentryExceptionConverter
 *
 * @author Kirill Sulim
 */
public class SentryExceptionConverter {

    public static SentryException convert(Container container) {
        String type = ContainerUtil.extractRequired(container, StackTraceFields.TYPE_FIELD);
        String value = ContainerUtil.extractRequired(container, StackTraceFields.VALUE_FIELD);
        String module = ContainerUtil.extractRequired(container, StackTraceFields.EXCEPTION_MODULE_FIELD);
        Container[] stacktrace = ContainerUtil.extractRequired(container, StackTraceFields.STACKTRACE_FIELD);

        return new SentryException(
                value,
                type,
                module,
                SentryStackTraceInterfaceConverter.convert(stacktrace)
        );
    }
}
