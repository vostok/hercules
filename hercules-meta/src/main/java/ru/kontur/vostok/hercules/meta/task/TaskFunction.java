package ru.kontur.vostok.hercules.meta.task;

/**
 * @author Gregory Koshelev
 */
public interface TaskFunction {
    /**
     * Apply function to binary content of the task
     *
     * @param data is binary conent of the task
     * @return commit flag which is true if function processed without error
     */
    boolean apply(byte[] data);
}
