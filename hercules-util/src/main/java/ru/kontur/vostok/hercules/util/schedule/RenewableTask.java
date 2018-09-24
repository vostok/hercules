package ru.kontur.vostok.hercules.util.schedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Renewable task
 *
 * @author Gregory Koshelev
 */
public class RenewableTask implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(RenewableTask.class);

    private final AtomicLong lastUpdatedTimestamp = new AtomicLong(-1);
    private volatile boolean enabled = true;
    private final Runnable task;
    private final long heartbeatMillis;
    private final ScheduledExecutorService executor;

    RenewableTask(Runnable task, long heartbeatMillis, ScheduledExecutorService executor) {
        this.task = task;
        this.heartbeatMillis = heartbeatMillis;
        this.executor = executor;
    }

    /**
     * Force task renewing.
     * <p>
     * Heartbeats are refreshing on method invocation. Also, task renewing schedule the task.
     */
    public void renew() {
        if (!enabled) {
            return;
        }

        lastUpdatedTimestamp.set(System.currentTimeMillis());
        runAndSchedule();
    }

    /**
     * Try to schedule the task.
     *
     * @return true if task was scheduled successfully, false otherwise
     */
    public boolean schedule() {
        if (!enabled) {
            return false;
        }
        try {
            executor.schedule(this, heartbeatMillis * 2, TimeUnit.MILLISECONDS);
            return true;
        } catch (RejectedExecutionException e) {
            LOGGER.warn("Task was rejected", e);
            return false;
        }
    }

    /**
     * Disable the task.
     * <p>
     * Disabled task cannot be renewed or scheduled.
     */
    public void disable() {
        enabled = false;
    }

    /**
     * Re enable previously disabled task. Task should be scheduled again by calling schedule() or renew() methods.
     */
    public void enable() {
        enabled = true;
    }

    @Override
    public void run() {
        if (!enabled) {
            return;
        }

        long last = lastUpdatedTimestamp.get();
        long now = System.currentTimeMillis();
        if (now - last >= heartbeatMillis && lastUpdatedTimestamp.compareAndSet(last, now)) {
            runAndSchedule();
        }
    }

    private void runAndSchedule() {
        try {
            task.run();
        } finally {
            executor.schedule(this, heartbeatMillis * 2, TimeUnit.MILLISECONDS);
        }
    }
}
