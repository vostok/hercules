package ru.kontur.vostok.hercules.meta.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.curator.exception.CuratorInternalException;
import ru.kontur.vostok.hercules.curator.exception.CuratorUnknownException;
import ru.kontur.vostok.hercules.curator.result.CreationResult;
import ru.kontur.vostok.hercules.meta.serialization.SerializationException;

import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The local task queue is used to submit tasks to ZK queue and to check the task execution status.
 *
 * @author Gregory Koshelev
 */
public class TaskQueue<T> {
    private static Logger LOGGER = LoggerFactory.getLogger(TaskQueue.class);

    private final TaskRepository<T> repository;
    private final long delayMs;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(); // TODO: provide custom Thread Factory;

    public TaskQueue(TaskRepository<T> repository, long delayMs) {
        this.repository = repository;
        this.delayMs = delayMs;
    }

    /**
     * Submit the task to ZK queue and schedule the task execution status check.
     *
     * @param task    the task to be executed
     * @param name    the name of the task
     * @param timeout timeout for task execution
     * @param unit    time unit
     * @return task future to await task execution
     */
    public TaskFuture submit(T task, String name, long timeout, TimeUnit unit) {
        long expirationNanoTime = System.nanoTime() + unit.toNanos(timeout);
        try {
            CreationResult result = repository.create(task, name);
            if (!result.isSuccess()) {
                return TaskFuture.failed();
            }
            TaskFuture taskFuture = TaskFuture.inProgress(result.node(), expirationNanoTime);
            scheduleCheck(taskFuture);
            return taskFuture;
        } catch (SerializationException | CuratorInternalException | CuratorUnknownException ex) {
            LOGGER.error("Task creation failed with Exception", ex);
            return TaskFuture.failed(ex);
        }
    }

    public void stop(long timeout, TimeUnit unit) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(timeout, unit)) {
                LOGGER.warn("Cannot stop Task Queue properly due to internal executor's slow termination");
            }
        } catch (InterruptedException ex) {
            LOGGER.warn("Task Queue stopping was interrupted", ex);
            Thread.currentThread().interrupt();
        }
    }

    private void check(TaskFuture taskFuture) {
        if (System.nanoTime() > taskFuture.getExpirationNanoTime()) {
            taskFuture.expire();
            taskFuture.awake();
            return;
        }

        try {
            if (!repository.exists(taskFuture.getTaskFullName())) {
                taskFuture.done();
                taskFuture.awake();
            }
        } catch (CuratorUnknownException ex) {
            LOGGER.error("Task check failed with exception", ex);
        } finally {
            scheduleCheck(taskFuture);
        }
    }

    private void scheduleCheck(TaskFuture taskFuture) {
        if (taskFuture.isInProgress()) {
            try {
                executor.schedule(() -> check(taskFuture), delayMs, TimeUnit.MILLISECONDS);
            } catch (RejectedExecutionException ex) {
                taskFuture.cancel();
                LOGGER.warn("Task check scheduling rejected due to exception", ex);
            }
        }
    }
}
