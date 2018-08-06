package ru.kontur.vostok.hercules.sentry.sink.converters;

import io.sentry.event.interfaces.SentryStackTraceElement;
import io.sentry.event.interfaces.StackTraceInterface;
import ru.kontur.vostok.hercules.protocol.Container;

import java.util.Arrays;

/**
 * SentryStackTraceInterfaceConverter
 *
 * @author Kirill Sulim
 */
public class SentryStackTraceInterfaceConverter {

    public static StackTraceInterface convert(Container[] containers) {
        return new StackTraceInterface(Arrays.stream(containers)
                .map(SentryStackTraceElementConverter::convert)
                .toArray(SentryStackTraceElement[]::new)
        );
    }
}
