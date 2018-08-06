package ru.kontur.vostok.hercules.sentry.sink.converters;

import io.sentry.event.interfaces.SentryException;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;

/**
 * SentryExceptionConverter
 *
 * @author Kirill Sulim
 */
public class SentryExceptionConverter {

    private static final String TYPE_FIELD_NAME = "type";
    private static final String VALUE_FIELD_NAME = "value";
    private static final String MODULE_FIELD_NAME = "module";
    private static final String STACKTRACE_FIELD_NAME = "stacktrace";

    public static SentryException convert(Container container) {
        String type = ContainerUtil.extractRequired(container, TYPE_FIELD_NAME, Type.STRING);
        String value = ContainerUtil.extractRequired(container, VALUE_FIELD_NAME, Type.TEXT);
        String module = ContainerUtil.extractRequired(container, MODULE_FIELD_NAME, Type.TEXT);
        Container[] stacktrace = ContainerUtil.extractRequired(container, STACKTRACE_FIELD_NAME, Type.CONTAINER_ARRAY);

        return new SentryException(
                value,
                type,
                module,
                SentryStackTraceInterfaceConverter.convert(stacktrace)
        );
    }
}
