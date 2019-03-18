package ru.kontur.vostok.hercules.util.schedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class Scheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(Scheduler.class);

    private final ScheduledExecutorService executor;

    public Scheduler(int threadPoolSize) {
        executor = Executors.newScheduledThreadPool(threadPoolSize);
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

    public void shutdown(long timeout, TimeUnit unit) {
        executor.shutdown();
        try {
            boolean terminated = executor.awaitTermination(timeout, unit);//TODO: Log unsuccessful termination
        } catch (InterruptedException e) {
            LOGGER.warn("Shutdown interrupted", e);
        }
    }
}
