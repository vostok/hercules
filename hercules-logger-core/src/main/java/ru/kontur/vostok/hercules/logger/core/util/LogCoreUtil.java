package ru.kontur.vostok.hercules.logger.core.util;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author Daniil Zhenikhov
 */
public final class LogCoreUtil {
    /**
     * Consume recursive chain of exceptions.
     *
     * @param exception Top exception
     */
    public static <TException> void consumeExceptionsChain(
            TException exception,
            Consumer<TException> consumer,
            Function<TException, TException> getCauseFunction) {
        TException currentException = exception;

        while (currentException != null) {
            consumer.accept(currentException);
            currentException = getCauseFunction.apply(currentException);
        }
    }

    private LogCoreUtil() {
    }
}
