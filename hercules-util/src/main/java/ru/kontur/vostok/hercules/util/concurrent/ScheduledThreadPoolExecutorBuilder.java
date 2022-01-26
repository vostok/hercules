package ru.kontur.vostok.hercules.util.concurrent;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class provides an API to configure and to create an instance of {@link ScheduledThreadPoolExecutor}.
 * <ul>Note, by default:
 *  <li>the value of {@code threadPoolSize} is 1,</li>
 *  <li>each new thread is created as a non-daemon thread,</li>
 *  <li>new threads have names accessible via {@link Thread#getName} of
 *      <em>hercules-pool-N-thread-M</em>, where <em>N</em> is the sequence
 *      number of this builder, and <em>M</em> is the sequence number
 *      of the thread created by {@link ScheduledThreadPoolExecutor}.</li>
 * </ul>
 *
 * @author Anton Akkuzin
 */
public final class ScheduledThreadPoolExecutorBuilder {

    private static final AtomicInteger poolNumber = new AtomicInteger(1);

    private int threadPoolSize = 1;
    private boolean continueExistingPeriodicTasksAfterShutdown = false;
    private boolean executeExistingDelayedTasksAfterShutdown = true;
    private boolean daemonThreads = false;
    private String threadNamePrefix;

    public ScheduledThreadPoolExecutor build() {
        if (threadNamePrefix == null) {
            threadNamePrefix = "hercules-pool-" + poolNumber.getAndIncrement();
        }

        ScheduledThreadPoolExecutor executor
                = new ScheduledThreadPoolExecutor(threadPoolSize, new NamedThreadFactory(threadNamePrefix, daemonThreads));

        executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(continueExistingPeriodicTasksAfterShutdown);
        executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(executeExistingDelayedTasksAfterShutdown);

        return executor;
    }

    /**
     * Sets a thread name prefix.
     * <p>Example: if threadPoolName param is <em>foo</em></p> then new threads will have names <em>foo-thread-M</em>.
     *
     * @param threadPoolName the thread name prefix
     * @return a reference to this object.
     */
    public ScheduledThreadPoolExecutorBuilder name(@NotNull String threadPoolName) {
        this.threadNamePrefix = threadPoolName;
        return this;
    }

    /**
     * Sets whether threads will be daemons.
     *
     * @param daemon if {@code true} then executor will create daemon threads, otherwise non-daemon threads
     * @return a reference to this object.
     */
    public ScheduledThreadPoolExecutorBuilder daemon(boolean daemon) {
        this.daemonThreads = daemon;
        return this;
    }

    /**
     * Sets the number of threads to keep in the pool.
     *
     * @param threadPoolSize the number of threads
     * @return a reference to this object.
     */
    public ScheduledThreadPoolExecutorBuilder threadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
        return this;
    }

    /**
     * @see ScheduledThreadPoolExecutor#setContinueExistingPeriodicTasksAfterShutdownPolicy(boolean)
     * @return a reference to this object.
     */
    public ScheduledThreadPoolExecutorBuilder continuePeriodicTasksAfterShutdown() {
        continueExistingPeriodicTasksAfterShutdown = true;
        return this;
    }

    /**
     * @see ScheduledThreadPoolExecutor#setExecuteExistingDelayedTasksAfterShutdownPolicy(boolean)
     * @return a reference to this object.
     */
    public ScheduledThreadPoolExecutorBuilder dropDelayedTasksAfterShutdown() {
        executeExistingDelayedTasksAfterShutdown = false;
        return this;
    }
}
