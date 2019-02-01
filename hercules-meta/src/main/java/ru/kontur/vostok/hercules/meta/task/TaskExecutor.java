package ru.kontur.vostok.hercules.meta.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
    private final long pollingTimeoutMillis;

    protected TaskExecutor(TaskRepository<T> repository, long pollingTimeoutMillis) {
        this.repository = repository;
        this.pollingTimeoutMillis = pollingTimeoutMillis;
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
                        LOGGER.warn("Get children fails with exception", e);
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
                        } catch (Exception e) {
                            repository.delete(protoTask.fullName);
                            continue;
                        }
                        if (task.isPresent() && execute(task.get())) {
                            repository.delete(protoTask.fullName);
                        } else {
                            awaitTimeout(mutex);
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
                repository.delete(task);
                continue;
            }
            Integer sequenceId;
            try {
                sequenceId = Integer.valueOf(task.substring(delimiterPosition + TaskConstants.SEQUENCE_DELIMITER.length()));
            } catch (NumberFormatException ex) {
                /* Never possible by hercules modules. Threat as self-healing */
                repository.delete(task);
                continue;
            }
            protoTasks.add(new ProtoTask(task, sequenceId));
        }

        return protoTasks;
    }

    /**
     * Await for polling timeout on mutex
     */
    private void awaitTimeout(Object mutex) {
        try {
            mutex.wait(pollingTimeoutMillis);
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
