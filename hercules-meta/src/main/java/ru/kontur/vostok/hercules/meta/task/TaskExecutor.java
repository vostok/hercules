package ru.kontur.vostok.hercules.meta.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.meta.curator.exception.CuratorException;
import ru.kontur.vostok.hercules.meta.curator.exception.CuratorInternalException;
import ru.kontur.vostok.hercules.meta.curator.exception.CuratorUnknownException;
import ru.kontur.vostok.hercules.meta.serialization.DeserializationException;

import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * @author Gregory Koshelev
 */
public abstract class TaskExecutor<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskExecutor.class);

    private volatile boolean running = false;

    //TODO: Must be used for latency optimization
    private final Object mutex = new Object();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final TaskRepository<T> repository;
    private final long pollTimeoutMillis;

    protected TaskExecutor(TaskRepository<T> repository, long pollTimeoutMillis) {
        this.repository = repository;
        this.pollTimeoutMillis = pollTimeoutMillis;
    }

    public void start() {
        running = true;
        executorService.submit(() -> {
            while (running) {
                synchronized (mutex) {
                    List<String> children;
                    try {
                        children = repository.list();
                    } catch (Exception e) {
                        LOGGER.error("Get children fails with exception", e);
                        awaitTimeout(mutex);
                        continue;
                    }
                    if (children.isEmpty()) {
                        awaitTimeout(mutex);
                        continue;
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
                            awaitTimeout(mutex);
                            break;
                        }

                        if (task.isPresent() && !execute(task.get())) {
                            awaitTimeout(mutex);
                            break;
                        }

                        try {
                            repository.delete(protoTask.fullName);
                        } catch (CuratorException e) {
                            LOGGER.warn("Task cannot be deleted", e);
                            awaitTimeout(mutex);//TODO: How to process this situation properly?
                            break;
                        }
                    }
                }
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

    protected abstract boolean execute(T task);

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
                Integer sequenceId = Integer.valueOf(task.substring(delimiterPosition + TaskConstants.SEQUENCE_DELIMITER.length()));
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
     * @param fullname the full name of invalid task
     */
    private void cleanInvalidTask(String fullname) {
        try {
            repository.delete(fullname);
        } catch (CuratorException e) {
            LOGGER.info("Cannot delete invalid task '" + fullname + "'", e);
        }
    }

    /**
     * Await for polling timeout on mutex
     */
    private void awaitTimeout(Object mutex) {
        try {
            mutex.wait(pollTimeoutMillis);
        } catch (InterruptedException interruptedException) {
            LOGGER.warn("Awaiting was interrupted", interruptedException);
        }
    }

    /**
     * ProtoTask represent tasks's fullName and sequenceId
     */
    private static final class ProtoTask implements Comparable<ProtoTask> {
        private final String fullName;
        private final Integer sequenceId;

        public ProtoTask(String fullName, Integer sequenceId) {
            this.fullName = fullName;
            this.sequenceId = sequenceId;
        }

        @Override
        public int compareTo(ProtoTask o) {
            return sequenceId.compareTo(o.sequenceId);
        }
    }
}
