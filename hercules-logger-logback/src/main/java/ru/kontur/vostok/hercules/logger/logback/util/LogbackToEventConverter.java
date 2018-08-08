package ru.kontur.vostok.hercules.logger.logback.util;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ru.kontur.vostok.hercules.gateway.client.EventPublisherFactory;
import ru.kontur.vostok.hercules.logger.core.LogEventBuilder;
import ru.kontur.vostok.hercules.logger.core.LogExceptionBuilder;
import ru.kontur.vostok.hercules.logger.core.util.LogCoreUtil;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Variant;

import java.util.Objects;

/**
 * Converter for logback {@link ILoggingEvent logs}
 *
 * @author Daniil Zhenikhov
 */
public class LogbackToEventConverter {
    private static final String PROJECT_TAG = "project";
    private static final String ENVIRONMENT_TAG = "env";

    public static Event createEvent(ILoggingEvent loggingEvent) {
        LogEventBuilder builder = new LogEventBuilder()
                .setLevel(loggingEvent.getLevel().levelStr)
                .setMessage(loggingEvent.getFormattedMessage())
                .setProperty(PROJECT_TAG, Variant.ofString(EventPublisherFactory.getProject()))
                .setProperty(ENVIRONMENT_TAG, Variant.ofString(EventPublisherFactory.getEnvironment()));

        if (Objects.nonNull(loggingEvent.getThrowableProxy())) {
            LogCoreUtil.consumeExceptionsChain(
                    loggingEvent.getThrowableProxy(),
                    throwableProxy -> addExceptionToBuilder(builder, throwableProxy),
                    IThrowableProxy::getCause
            );
        }

        loggingEvent.getMDCPropertyMap()
                .forEach((key, value) -> builder.setProperty(key, Variant.ofString(value)));

        return builder.build();
    }

    private static void addExceptionToBuilder(LogEventBuilder builder, IThrowableProxy throwableProxy) {
        LogExceptionBuilder exceptionBuilder = builder.startException()
                .setType(throwableProxy.getClassName())
                .setMessage(throwableProxy.getMessage());

        if (throwableProxy.getStackTraceElementProxyArray().length > 0) {
            String module = throwableProxy
                    .getStackTraceElementProxyArray()[0]
                    .getStackTraceElement()
                    .getClassName();

            exceptionBuilder.setModule(module);

            for (StackTraceElementProxy stackTraceElementProxy: throwableProxy.getStackTraceElementProxyArray()) {
                StackTraceElement stackTraceElement = stackTraceElementProxy.getStackTraceElement();

                exceptionBuilder.startStackTraceElement()
                        .setFile(stackTraceElement.getFileName())
                        .setFunction(stackTraceElement.getMethodName())
                        .setLine(stackTraceElement.getLineNumber())
                        .setSource(stackTraceElement.getClassName())
                        .endStackTraceElement();
            }
        }
        exceptionBuilder.endException();

    }
}

