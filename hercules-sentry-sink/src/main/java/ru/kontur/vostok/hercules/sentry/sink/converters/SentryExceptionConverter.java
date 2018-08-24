package ru.kontur.vostok.hercules.sentry.sink.converters;

import io.sentry.event.interfaces.SentryException;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.protocol.util.FieldDescription;

/**
 * SentryExceptionConverter
 *
 * @author Kirill Sulim
 */
public class SentryExceptionConverter {

    private static final FieldDescription TYPE_FIELD = FieldDescription.create( "tp", Type.STRING);
    private static final FieldDescription VALUE_FIELD = FieldDescription.create("msg", Type.TEXT);
    private static final FieldDescription MODULE_FIELD = FieldDescription.create("mod", Type.TEXT);
    public static final FieldDescription STACKTRACE_FIELD = FieldDescription.create("str", Type.CONTAINER_ARRAY);

    public static SentryException convert(Container container) {
        String type = ContainerUtil.extractRequired(container, TYPE_FIELD);
        String value = ContainerUtil.extractRequired(container, VALUE_FIELD);
        String module = ContainerUtil.extractRequired(container, MODULE_FIELD);
        Container[] stacktrace = ContainerUtil.extractRequired(container, STACKTRACE_FIELD);

        return new SentryException(
                value,
                type,
                module,
                SentryStackTraceInterfaceConverter.convert(stacktrace)
        );
    }
}
