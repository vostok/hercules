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
    public static ThreadFactory newNamedThreadFactory(String name) {
        return new NamedThreadFactory(name, true);
    }

    private ThreadFactories() {
        /* static class */
    }
}
