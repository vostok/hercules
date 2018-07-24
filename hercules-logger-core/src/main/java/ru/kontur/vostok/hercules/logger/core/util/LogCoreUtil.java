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
    public static <E> void consumeExceptionsChain(
            E exception,
            Consumer<E> consumer,
            Function<E, E> getCauseFunction) {
        E currentException = exception;

        while (currentException != null) {
            consumer.accept(currentException);
            currentException = getCauseFunction.apply(currentException);
        }
    }

    private LogCoreUtil() {
    }
}
