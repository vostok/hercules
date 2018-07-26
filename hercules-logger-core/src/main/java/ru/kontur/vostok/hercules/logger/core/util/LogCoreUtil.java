package ru.kontur.vostok.hercules.logger.core.util;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

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

    public static Supplier<RuntimeException> missingLoggerConfiguration(String confName) {
        return () -> new RuntimeException("Missing '" + confName + "' configuration");
    }

    public static void requireNotNullConfiguration(Object conf, String confName) {
        if (Objects.isNull(conf)) {
            throw missingLoggerConfiguration(confName).get();
        }
    }

    private LogCoreUtil() {
    }
}
