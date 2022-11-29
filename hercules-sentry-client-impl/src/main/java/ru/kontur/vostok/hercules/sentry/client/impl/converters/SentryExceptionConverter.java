package ru.kontur.vostok.hercules.sentry.client.impl.converters;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model.SentryException;
import ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model.SentryStackTrace;
import ru.kontur.vostok.hercules.tags.ExceptionTags;

/**
 * Allows convert exception details from a Hercules event to a Sentry exception interface
 *
 * @author Tatyana Tokmyanina
 */
public class SentryExceptionConverter {
    public static List<SentryException> convertException(Container container) {
        List<SentryException> exceptions = new ArrayList<>(16);
        convertException(container, exceptions);
        return exceptions;
    }

    private static void convertException(final Container currentException,
            final List<SentryException> converted) {
        converted.add(SentryExceptionConverter.convert(currentException));
        ContainerUtil.extract(currentException, ExceptionTags.INNER_EXCEPTIONS_TAG)
                .ifPresent(exceptions -> Arrays.stream(exceptions)
                        .forEach(exception -> convertException(exception, converted)));
    }

    private static SentryException convert(final Container exception) {
        SentryException sentryException = new SentryException();
        ContainerUtil.extract(exception, ExceptionTags.TYPE_TAG)
                .ifPresent(sentryException::setType);
        ContainerUtil.extract(exception, ExceptionTags.MESSAGE_TAG)
                .ifPresent(sentryException::setValue);
        ContainerUtil.extract(exception, ExceptionTags.MODULE_TAG)
                .ifPresent(sentryException::setModule);
        ContainerUtil.extract(exception, ExceptionTags.STACK_FRAMES)
                .ifPresent(value -> {
                    SentryStackTrace stackTrace = SentryStackTraceConverter.convert(value);
                    sentryException.setStacktrace(stackTrace);
                });
        return sentryException;
    }
}
