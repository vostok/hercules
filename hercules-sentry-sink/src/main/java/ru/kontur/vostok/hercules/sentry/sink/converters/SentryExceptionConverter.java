package ru.kontur.vostok.hercules.sentry.sink.converters;

import io.sentry.event.interfaces.SentryException;
import io.sentry.event.interfaces.SentryStackTraceElement;
import io.sentry.event.interfaces.StackTraceInterface;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.tags.ExceptionTags;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;

import java.util.Arrays;
import java.util.Optional;

/**
 * SentryExceptionConverter
 * Allows to convert exception details from a Hercules event to a Sentry event
 *
 * @author Kirill Sulim
 */
public class SentryExceptionConverter {

    private static final int NOT_FOUND = -1;

    /**
     * Convert exception details from a Hercules event to a Sentry event
     *
     * @param container the container with values of the exception tags of a Hercules event
     * @return the Sentry exception
     */
    public static SentryException convert(final Container container) {
        final String message = ContainerUtil.extract(container, ExceptionTags.MESSAGE_TAG).orElse(null);

        final Optional<ClassPackagePair> classPackagePair = ContainerUtil.extract(container, ExceptionTags.TYPE_TAG)
            .map(SentryExceptionConverter::extractClassPackagePair);

        final String className = classPackagePair.map(ClassPackagePair::getClassName).orElse(null);
        final String packageName = classPackagePair.map(ClassPackagePair::getPackageName).orElse(null);

        final StackTraceInterface stacktrace = ContainerUtil.extract(container, ExceptionTags.STACK_FRAMES)
            .map(containers -> Arrays.stream(containers)
                .map(SentryStackTraceElementConverter::convert)
                .toArray(SentryStackTraceElement[]::new)
            )
            .map(StackTraceInterface::new)
            .orElse(null);

        return new SentryException(
                message,
                className,
                packageName,
                stacktrace
        );
    }

    private static ClassPackagePair extractClassPackagePair(final String typeName) {
        final int finalDot = typeName.lastIndexOf('.');

        if (finalDot != NOT_FOUND) {
            return new ClassPackagePair(typeName.substring(0, finalDot), typeName.substring(finalDot + 1));
        } else {
            return new ClassPackagePair(null, typeName);
        }
    }

    private static class ClassPackagePair {
        private final String packageName;
        private final String className;

        public ClassPackagePair(String packageName, String className) {
            this.packageName = packageName;
            this.className = className;
        }

        public String getPackageName() {
            return packageName;
        }

        public String getClassName() {
            return className;
        }
    }
}
