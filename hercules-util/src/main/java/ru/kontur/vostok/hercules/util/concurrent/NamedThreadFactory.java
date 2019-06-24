package ru.kontur.vostok.hercules.util.concurrent;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Gregory Koshelev
 */
public class NamedThreadFactory implements ThreadFactory {
    private final String prefix;
    private final boolean daemon;
    private final AtomicInteger threadNumber = new AtomicInteger(1);

    public NamedThreadFactory(String name, boolean daemon) {
        this.prefix = name + "-thread-";
        this.daemon = daemon;
    }

    @Override
    public Thread newThread(@NotNull Runnable runnable) {
        final Thread thread = new Thread(runnable, prefix + threadNumber.getAndIncrement());
        if (daemon) {
            thread.setDaemon(true);
        }
        return thread;
    }
}
