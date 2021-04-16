package ru.kontur.vostok.hercules.meta.task;

import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.curator.LatchWatcher;
import ru.kontur.vostok.hercules.curator.exception.CuratorException;
import ru.kontur.vostok.hercules.meta.serialization.DeserializationException;
import ru.kontur.vostok.hercules.util.concurrent.ThreadFactories;

import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Gregory Koshelev
 */
public abstract class TaskExecutor<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskExecutor.class);

    private volatile boolean running = false;
    private final AtomicReference<State> state = new AtomicReference<>(State.SHOULD_POLL);
    private final Object mutex = new Object();
    private final ExecutorService executorService =
            Executors.newSingleThreadExecutor(ThreadFactories.newNamedThreadFactory("task-executor", false));

    private final TaskRepository<T> repository;
    private final long pollTimeoutMillis;

    private final LatchWatcher latchWatcher;

    protected TaskExecutor(TaskRepository<T> repository, long pollTimeoutMillis) {
        this.repository = repository;
        this.pollTimeoutMillis = pollTimeoutMillis;

        this.latchWatcher = new LatchWatcher(event -> {
            if (event.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
                if (state.compareAndSet(State.WAITING, State.SHOULD_POLL)) {
                    synchronized (mutex) {
                        mutex.notify();
                    }
                }
            }
        });
    }

    public void start() {
        running = true;
        executorService.submit(() -> {
            while (running) {
                poll();
                awaitTimeout();
            }
        });
    }

    public boolean stop(long timeout, TimeUnit unit) {
        running = false;
        try {
            return executorService.awaitTermination(timeout, unit);
        } catch (InterruptedException e) {
            LOGGER.error("TaskExecutor shutdown execute was terminated by InterruptedException", e);
            return false;
        }
    }

    /**
     * Execute task and return {@code true} if task should be removed
     * (task has been processed successfully or no retry is needed).
     *
     * @param task task
     * @return {@code true} if task should be removed, {@code false} if task should be retried
     */
    protected abstract boolean execute(T task);

    private void poll() {
        state.set(State.POLLING);

        List<String> children;
        try {
            children = latchWatcher.latch() ? repository.list(latchWatcher) : repository.list();
        } catch (Exception e) {
            latchWatcher.release();
            state.set(State.WAITING);
            LOGGER.error("Get children fails with exception", e);
            return;
        }
        if (children.isEmpty()) {
            state.compareAndSet(State.POLLING, State.WAITING);
            return;
        }

        SortedSet<ProtoTask> protoTasks = preprocess(children);

        for (ProtoTask protoTask : protoTasks) {
            Optional<T> task;
            try {
                task = repository.read(protoTask.fullName);
            } catch (DeserializationException e) {
                LOGGER.warn("Task deserialization exception", e);
                cleanInvalidTask(protoTask.fullName);
                continue;
            } catch (CuratorException e) {
                LOGGER.error("Cannot read Task from repository", e);
                state.set(State.WAITING);
                return;
            }

            if (task.isPresent() && !execute(task.get())) {
                state.set(State.SHOULD_POLL);
                return;
            }

            try {
                repository.delete(protoTask.fullName);
            } catch (CuratorException e) {
                LOGGER.error("Task cannot be deleted", e);
                state.set(State.WAITING);
                return;
            }
        }

        state.set(State.SHOULD_POLL);
    }

    /**
     * Sort tasks by sequenceId asc. Also, remove invalid tasks
     *
     * @param tasks are source tasks fullNames
     * @return sorted tasks
     */
    private SortedSet<ProtoTask> preprocess(List<String> tasks) {
        SortedSet<ProtoTask> protoTasks = new TreeSet<>();

        for (String task : tasks) {
            int delimiterPosition = task.indexOf(TaskConstants.SEQUENCE_DELIMITER);
            if (delimiterPosition == -1) {
                /* Never possible by hercules modules. Treat as self-healing */
                cleanInvalidTask(task);
                continue;
            }

            try {
                int sequenceId = Integer.parseInt(task.substring(delimiterPosition + TaskConstants.SEQUENCE_DELIMITER.length()));
                protoTasks.add(new ProtoTask(task, sequenceId));
            } catch (NumberFormatException ex) {
                /* Never possible by hercules modules. Threat as self-healing */
                cleanInvalidTask(task);
            }
        }

        return protoTasks;
    }

    /**
     * Delete invalid task from task list. All possible exceptions are ignored.
     *
     * @param fullName the full name of invalid task
     */
    private void cleanInvalidTask(String fullName) {
        try {
            repository.delete(fullName);
        } catch (CuratorException e) {
            LOGGER.warn("Cannot delete invalid task '" + fullName + "'", e);
        }
    }

    /**
     * Await for polling timeout on mutex
     */
    private void awaitTimeout() {
        if (state.get() != State.WAITING) {
            return;
        }

        synchronized (mutex) {
            try {
                mutex.wait(pollTimeoutMillis);
            } catch (InterruptedException interruptedException) {
                LOGGER.warn("Awaiting was interrupted", interruptedException);
            }
        }
    }

    /**
     * A possible state of a task executor.
     */
    private enum State {
        /**
         * A task executor is waiting when new tasks to become available.
         */
        WAITING,
        /**
         * A task executor is processing tasks.
         */
        POLLING,
        /**
         * A task executor should poll ZK queue for new tasks.
         */
        SHOULD_POLL;
    }

    /**
     * ProtoTask represent tasks's fullName and sequenceId
     */
    private static final class ProtoTask implements Comparable<ProtoTask> {
        /**
         * The full name of the zk node (including path to the root)
         */
        private final String fullName;
        /**
         * The sequence id of the zk node which is a signed 32-bit integer
         * where {@link Integer#MIN_VALUE} follows by {@link Integer#MAX_VALUE}.
         */
        private final int sequenceId;

        public ProtoTask(String fullName, int sequenceId) {
            this.fullName = fullName;
            this.sequenceId = sequenceId;
        }

        /**
         * Compares this task with the specified task for order.
         * <p>
         * Since sequence id rotates, sequence ids are compared as follows:
         * <ul>
         *     <li>{@code x > y} if {@code (x - y) > 0},</li>
         *     <li>{@code x = y} if {@code (x - y) mod 2^31 = 0},</li>
         *     <li>{@code x < y} if {@code (x - y) < 0}.</li>
         * </ul>
         * <p>
         * Note: {@code x} and {@code y} are treated as equal if {@code x - y = -2^31}.
         * There is an assumption that task queue contains at most {@link Integer#MAX_VALUE} elements.
         * <p>
         * Samples:
         * <pre>
         * x = 42, y = 42 -> x - y = 0 -> x = y
         * x = 5, y = 10 -> x - y = -5 -> x < y
         * x = 0, y = -1 -> x - y = 1 -> x > y
         * x = 2^31 - 1, y = -2^31 -> x - y = -1 (due to int overflow) -> x < y
         * x = 0, y = -2^31 -> x - y = -2^31 (and 0 by mod 2^31) -> x = y
         * </pre>
         *
         * @param o the specified task to be compared
         * @return a negative integer, zero, or a positive integer
         * as this task is less than, equal to, or greater than the specified task.
         */
        @Override
        public int compareTo(ProtoTask o) {
            int result = sequenceId - o.sequenceId;
            return (result != Integer.MIN_VALUE) ? result : 0;
        }
    }
}
