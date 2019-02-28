package ru.kontur.vostok.hercules.meta.task;

import java.util.concurrent.TimeUnit;

/**
 * TaskFuture is used to await the task execution
 * <p>
 * TaskFuture has several possible statuses:<br>
 * - {@link TaskFutureStatus#IN_PROGRESS} - active task<br>
 * - {@link TaskFutureStatus#FAILED} - failed task creation<br>
 * - {@link TaskFutureStatus#DONE} - processed task<br>
 * - {@link TaskFutureStatus#CANCELLED} - cancelled task future<br>
 * - {@link TaskFutureStatus#EXPIRED} - expired task future<br>
 *
 * @author Gregory Koshelev
 */
public class TaskFuture {
    private final Object mutex = new Object();

    private final String taskFullName;
    private final long expirationNanoTime;
    private volatile TaskFutureStatus status;
    private volatile Exception exception;

    private TaskFuture(String taskFullName, long expirationNanoTime, TaskFutureStatus status, Exception exception) {
        this.taskFullName = taskFullName;
        this.expirationNanoTime = expirationNanoTime;
        this.status = status;
        this.exception = exception;
    }

    public boolean isInProgress() {
        return status == TaskFutureStatus.IN_PROGRESS;
    }

    public boolean isFailed() {
        return status == TaskFutureStatus.FAILED;
    }

    public boolean isDone() {
        return status == TaskFutureStatus.DONE;
    }

    public boolean isCancelled() {
        return status == TaskFutureStatus.CANCELLED;
    }

    public boolean isExpired() {
        return status == TaskFutureStatus.EXPIRED;
    }

    public TaskFutureStatus status() {
        return status;
    }

    public Exception exception() {
        return exception;
    }

    /**
     * Task execution awaiting. Awaiting timeout is bounded by task future timeout.<br>
     * Also, awaiting thread can be awaked by invocation of {@link #awake()} method.
     *
     * @return {@code true} if task future was completed, {@code false} otherwise
     */
    public boolean await() {
        if (!isInProgress()) {
            return true;
        }
        long nanoTime;
        synchronized (mutex) {
            while (isInProgress() && (nanoTime = System.nanoTime()) < expirationNanoTime) {
                try {
                    mutex.wait(TimeUnit.NANOSECONDS.toMillis(expirationNanoTime - nanoTime));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return !isInProgress();
                }
            }
        }
        return !isInProgress();
    }

    /**
     * Notify awaiting thread on {@link #await()} method.
     */
    void awake() {
        synchronized (mutex) {
            mutex.notify();
        }
    }

    String getTaskFullName() {
        return taskFullName;
    }

    long getExpirationNanoTime() {
        return expirationNanoTime;
    }

    void done() {
        status = TaskFutureStatus.DONE;
    }

    void cancel() {
        status = TaskFutureStatus.CANCELLED;
    }

    void expire() {
        status = TaskFutureStatus.EXPIRED;
    }

    static TaskFuture failed(Exception exception) {
        return new TaskFuture(null, 0, TaskFutureStatus.FAILED, exception);
    }

    static TaskFuture failed() {
        return new TaskFuture(null, 0, TaskFutureStatus.FAILED, null);
    }

    static TaskFuture inProgress(String taskFullName, long expirationNanoTime) {
        return new TaskFuture(taskFullName, expirationNanoTime, TaskFutureStatus.IN_PROGRESS, null);
    }
}
