package ru.kontur.vostok.hercules.util.concurrent;

import java.util.concurrent.ThreadFactory;

/**
 * @author Gregory Koshelev
 */
public final class ThreadFactories {
    /**
     * Returns new {@link NamedThreadFactory} instance with specified thread name prefix. Threads will be daemons.
     *
     * @param name the thread name prefix
     * @return {@link ThreadFactory} that creates daemon threads with specified name prefix
     */
    public static ThreadFactory newDaemonNamedThreadFactory(String name) {
        return newNamedThreadFactory(name, true);
    }

    /**
     * Returns new {@link NamedThreadFactory} instance with specified thread name prefix.
     *
     * @param name   the thread name prefix
     * @param daemon a flag determines if thread factory produces daemon threads
     * @return {@link ThreadFactory} that creates threads with specified name prefix
     */
    public static ThreadFactory newNamedThreadFactory(String name, boolean daemon) {
        return new NamedThreadFactory(name, daemon);
    }

    private ThreadFactories() {
        /* static class */
    }
}
