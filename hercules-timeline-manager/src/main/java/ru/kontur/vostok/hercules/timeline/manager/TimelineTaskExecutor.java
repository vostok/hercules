package ru.kontur.vostok.hercules.timeline.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.meta.task.TaskExecutor;
import ru.kontur.vostok.hercules.meta.task.timeline.TimelineTask;
import ru.kontur.vostok.hercules.meta.task.timeline.TimelineTaskRepository;
import ru.kontur.vostok.hercules.meta.timeline.TimelineRepository;

/**
 * @author Gregory Koshelev
 */
public class TimelineTaskExecutor extends TaskExecutor<TimelineTask> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TimelineTaskExecutor.class);

    private final CassandraManager cassandraManager;
    private final TimelineRepository timelineRepository;

    protected TimelineTaskExecutor(
            TimelineTaskRepository timelineTaskRepository,
            long pollingTimeoutMillis,
            CassandraManager cassandraManager,
            TimelineRepository timelineRepository) {
        super(timelineTaskRepository, pollingTimeoutMillis);
        this.cassandraManager = cassandraManager;
        this.timelineRepository = timelineRepository;
    }

    @Override
    protected boolean execute(TimelineTask task) {
        switch (task.getType()) {
            case CREATE:
                cassandraManager.createTable(task.getTimeline().getName());//TODO: process creation error
                LOGGER.info("Created table '{}'", task.getTimeline().getName());
                try {
                    timelineRepository.create(task.getTimeline());
                } catch (Exception e) {
                    LOGGER.error("Timeline creation failed with exception", e);
                    return false;
                }
                return true;
            case DELETE:
                try {
                    timelineRepository.delete(task.getTimeline().getName());
                } catch (Exception e) {
                    LOGGER.error("Timeline deletion failed with exception", e);
                    return false;
                }
                cassandraManager.deleteTable(task.getTimeline().getName());//TODO: process deletion error
                LOGGER.info("Deleted table '{}'", task.getTimeline().getName());
                return true;
            default:
                LOGGER.error("Unknown task type {}", task.getType());
                return false;
        }
    }
}
