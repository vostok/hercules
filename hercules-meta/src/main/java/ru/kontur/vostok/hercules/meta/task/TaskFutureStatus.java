package ru.kontur.vostok.hercules.meta.task;

/**
 * {@link TaskFuture} status
 *
 * @author Gregory Koshelev
 */
public enum TaskFutureStatus {
    /**
     * Active task
     */
    IN_PROGRESS,
    /**
     * Failed task (usually means creation error)
     */
    FAILED,
    /**
     * Processed task
     */
    DONE,
    /**
     * Cancelled TaskFuture
     */
    CANCELLED,
    /**
     * Expired TaskFuture
     */
    EXPIRED;
}
