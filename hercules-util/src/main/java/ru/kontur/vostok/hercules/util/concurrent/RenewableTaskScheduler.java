package ru.kontur.vostok.hercules.util.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.util.lifecycle.Stoppable;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class RenewableTaskScheduler implements Stoppable {

    private static final Logger LOGGER = LoggerFactory.getLogger(RenewableTaskScheduler.class);

    private final ScheduledExecutorService executor;

    public RenewableTaskScheduler(String name, int threadPoolSize) {
        executor = Executors.newScheduledThreadPool(threadPoolSize, ThreadFactories.newNamedThreadFactory(name, false));
    }

    /**
     * Create renewable task and schedule it if needed
     * <p>
     * If task is scheduled, it will run between two heartbeats. Task is auto-scheduled if call renew or run methods.
     *
     * @param runnable is task to be scheduled
     * @param heartbeatMillis is heartbeat interval in millis
     * @param shouldBeScheduled if true, then call schedule method on task
     * @return task created
     */
    public RenewableTask task(Runnable runnable, long heartbeatMillis, boolean shouldBeScheduled) {
        RenewableTask task = new RenewableTask(runnable, heartbeatMillis, executor);
        if (shouldBeScheduled) {
            task.schedule();
        }
        return task;
    }

    public boolean stop(long timeout, TimeUnit unit) {
        executor.shutdown();
        try {
            return executor.awaitTermination(timeout, unit);
        } catch (InterruptedException e) {
            LOGGER.warn("Shutdown interrupted", e);
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
