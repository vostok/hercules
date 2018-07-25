package ru.kontur.vostok.hercules.logger.log4j.util;

import org.apache.logging.log4j.core.LogEvent;
import ru.kontur.vostok.hercules.logger.core.LogEventBuilder;
import ru.kontur.vostok.hercules.logger.core.LogExceptionBuilder;
import ru.kontur.vostok.hercules.logger.core.util.LogCoreUtil;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Variant;

import java.util.Objects;

/**
 * Converter for log4j
 *
 * @author Daniil Zhenikhov
 */
public class Log4jToEventConverter {
    public static Event createEvent(LogEvent logEvent) {
        LogEventBuilder builder = new LogEventBuilder()
                .setLevel(logEvent.getLevel().toString())
                .setMessage(logEvent.getMessage().getFormattedMessage());

        if (Objects.nonNull(logEvent.getThrown())) {
            LogCoreUtil.consumeExceptionsChain(
                    logEvent.getThrown(),
                    throwable -> addExceptionToBuilder(builder, throwable),
                    Throwable::getCause);
        }

        logEvent.getContextData()
                .toMap()
                .forEach((key, value) -> builder.setProperty(key, Variant.ofString(value)));

        return builder.build();
    }

    private static void addExceptionToBuilder(LogEventBuilder builder, Throwable throwable) {
        LogExceptionBuilder exceptionBuilder = builder.startException()
                .setType(throwable.getClass().getName())
                .setMessage(throwable.getMessage());

        if (throwable.getStackTrace().length > 0) {
            exceptionBuilder.setModule(throwable.getStackTrace()[0].getClassName());

            for (StackTraceElement stackTraceElement: throwable.getStackTrace()) {
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
