package ru.kontur.vostok.hercules.meta.task.timeline;

import ru.kontur.vostok.hercules.meta.curator.CuratorClient;
import ru.kontur.vostok.hercules.meta.task.TaskRepository;

/**
 * @author Gregory Koshelev
 */
public final class TimelineTaskRepository extends TaskRepository<TimelineTask> {
    public TimelineTaskRepository(CuratorClient curatorClient) {
        super(curatorClient, TimelineTask.class, "/hercules/tasks/timelines");
    }
}
