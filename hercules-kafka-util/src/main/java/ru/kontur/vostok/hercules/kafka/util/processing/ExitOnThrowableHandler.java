package ru.kontur.vostok.hercules.kafka.util.processing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ExitOnThrowableHandler
 *
 * @author Kirill Sulim
 */
public class ExitOnThrowableHandler implements Thread.UncaughtExceptionHandler {

    public static final ExitOnThrowableHandler INSTANCE = new ExitOnThrowableHandler();

    private static final Logger LOGGER = LoggerFactory.getLogger(ExitOnThrowableHandler.class);

    private final AtomicBoolean exitCalled = new AtomicBoolean(false);

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        LOGGER.error("Uncaught error in thread '{}'", t.getName(), e);

        if (exitCalled.compareAndSet(false, true)) {
            LOGGER.error("Exiting due to uncaught error in thread '{}'", t.getName());
            new Thread(() -> System.exit(1)).start();
        }
    }

    private ExitOnThrowableHandler() {
    }
}
