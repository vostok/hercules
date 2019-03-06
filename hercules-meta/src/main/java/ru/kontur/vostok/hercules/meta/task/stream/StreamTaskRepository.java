package ru.kontur.vostok.hercules.meta.task.stream;

import ru.kontur.vostok.hercules.curator.CuratorClient;
import ru.kontur.vostok.hercules.meta.task.TaskRepository;

/**
 * @author Gregory Koshelev
 */
public final class StreamTaskRepository extends TaskRepository<StreamTask> {
    public StreamTaskRepository(CuratorClient curatorClient) {
        super(curatorClient, StreamTask.class, "/hercules/tasks/streams");
    }
}
