package ru.kontur.vostok.hercules.stream.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.meta.task.TaskExecutor;
import ru.kontur.vostok.hercules.meta.task.stream.StreamTask;
import ru.kontur.vostok.hercules.meta.task.stream.StreamTaskRepository;

/**
 * @author Gregory Koshelev
 */
public class StreamTaskExecutor extends TaskExecutor<StreamTask> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamTaskExecutor.class);

    private final KafkaManager kafkaManager;
    private final StreamRepository streamRepository;

    protected StreamTaskExecutor(StreamTaskRepository streamTaskRepository, long pollingTimeoutMillis, KafkaManager kafkaManager, StreamRepository streamRepository) {
        super(streamTaskRepository, pollingTimeoutMillis);
        this.kafkaManager = kafkaManager;
        this.streamRepository = streamRepository;
    }

    @Override
    protected boolean execute(StreamTask task) {
        switch (task.getType()) {
            case CREATE:
                kafkaManager.createTopic(task.getStream().getName(), task.getStream().getPartitions(), task.getStream().getTtl());//TODO: process creation error
                LOGGER.info("Created topic '{}'", task.getStream().getName());
                try {
                    streamRepository.create(task.getStream());
                } catch (Exception e) {
                    LOGGER.error("Stream creation failed with exception", e);
                    return false;
                }
                return true;
            case DELETE:
                try {
                    streamRepository.delete(task.getStream().getName());
                } catch (Exception e) {
                    LOGGER.error("Stream deletion failed with exception", e);
                    return false;
                }
                kafkaManager.deleteTopic(task.getStream().getName());//TODO: process deletion error
                LOGGER.info("Deleted topic '{}'", task.getStream().getName());
                return true;
            case INCREASE_PARTITIONS:
                kafkaManager.increasePartitions(task.getStream().getName(), task.getStream().getPartitions());//TODO: process error
                LOGGER.info("Increase partitions for topic '{}", task.getStream().getName());
                try {
                    streamRepository.update(task.getStream());
                } catch (Exception e) {
                    LOGGER.error("Stream update failed with exception", e);
                    return false;
                }
                return true;
            default:
                LOGGER.error("Unknown task type {}", task.getType());
                return false;
        }
    }
}
