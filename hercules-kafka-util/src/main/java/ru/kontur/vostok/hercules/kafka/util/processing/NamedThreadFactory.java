package ru.kontur.vostok.hercules.kafka.util.processing;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * NamedThreadFactory - thread factory that names each thread with prefix and id
 *
 * @author Kirill Sulim
 */
public class NamedThreadFactory implements ThreadFactory {

    private final AtomicInteger threadNumber = new AtomicInteger(0);
    private final String poolName;
    private final Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

    public NamedThreadFactory(String poolName) {
        this.poolName = poolName + "-";
        this.uncaughtExceptionHandler = ExitOnThrowableHandler.INSTANCE;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r, getNextThreadName());
        thread.setUncaughtExceptionHandler(uncaughtExceptionHandler);
        return thread;
    }

    private String getNextThreadName() {
        return poolName + threadNumber.getAndIncrement();
    }
}
