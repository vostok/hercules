package ru.kontur.vostok.hercules.stream.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.health.Meter;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.meta.task.TaskExecutor;
import ru.kontur.vostok.hercules.meta.task.stream.StreamTask;
import ru.kontur.vostok.hercules.meta.task.stream.StreamTaskRepository;
import ru.kontur.vostok.hercules.meta.task.stream.StreamTaskType;

import java.util.EnumMap;
import java.util.Map;

/**
 * @author Gregory Koshelev
 */
public class StreamTaskExecutor extends TaskExecutor<StreamTask> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamTaskExecutor.class);

    private final KafkaManager kafkaManager;
    private final StreamRepository streamRepository;
    private final Meter createdStreamCount;
    private final Meter deletedStreamCount;
    private final Meter updatedStreamCount;

    private final Map<StreamTaskType, StreamTaskProcessor> processors;

    protected StreamTaskExecutor(
            StreamTaskRepository streamTaskRepository,
            long pollingTimeoutMillis,
            KafkaManager kafkaManager,
            StreamRepository streamRepository,
            MetricsCollector metricsCollector) {
        super(streamTaskRepository, pollingTimeoutMillis);
        this.kafkaManager = kafkaManager;
        this.streamRepository = streamRepository;
        this.createdStreamCount = metricsCollector.meter("createdStreamCount");
        this.deletedStreamCount = metricsCollector.meter("deletedStreamCount");
        this.updatedStreamCount = metricsCollector.meter("updatedStreamCount");

        Map<StreamTaskType, StreamTaskProcessor> processors = new EnumMap<>(StreamTaskType.class);
        processors.put(StreamTaskType.CREATE, this::create);
        processors.put(StreamTaskType.DELETE, this::delete);
        processors.put(StreamTaskType.INCREASE_PARTITIONS, this::increasePartitions);
        processors.put(StreamTaskType.CHANGE_TTL, this::changeTtl);
        this.processors = processors;
    }

    @Override
    protected boolean execute(StreamTask task) {
        return processors.getOrDefault(task.getType(), this::unknown).process(task);
    }

    private boolean create(StreamTask task) {
        CreateTopicResult result = kafkaManager.createTopic(task.getStream().getName(), task.getStream().getPartitions(), task.getStream().getTtl());//TODO: process creation error
        if (result == CreateTopicResult.FAILED) {
            //Topic creation failed. Task will be deleted, since it difficult to determine cause of failure.
            return true;
        }
        LOGGER.info("Created topic '{}'", task.getStream().getName());
        try {
            streamRepository.create(task.getStream());
        } catch (Exception ex) {
            LOGGER.error("Stream creation failed with exception", ex);
            return false;
        }
        createdStreamCount.mark();
        return true;
    }

    private boolean delete(StreamTask task) {
        try {
            streamRepository.delete(task.getStream().getName());
        } catch (Exception ex) {
            LOGGER.error("Stream deletion failed with exception", ex);
            return false;
        }
        kafkaManager.deleteTopic(task.getStream().getName());//TODO: process deletion error
        LOGGER.info("Deleted topic '{}'", task.getStream().getName());
        deletedStreamCount.mark();
        return true;
    }

    private boolean increasePartitions(StreamTask task) {
        kafkaManager.increasePartitions(task.getStream().getName(), task.getStream().getPartitions());//TODO: process error
        LOGGER.info("Increase partitions for topic '{}", task.getStream().getName());
        try {
            streamRepository.update(task.getStream());
        } catch (Exception ex) {
            LOGGER.error("Stream update failed with exception", ex);
            return false;
        }
        updatedStreamCount.mark();
        return true;

    }

    private boolean changeTtl(StreamTask task) {
        kafkaManager.changeTtl(task.getStream().getName(), task.getStream().getTtl());
        LOGGER.info("Change ttl for topic '{}'", task.getStream().getName());
        try {
            streamRepository.update(task.getStream());
        } catch (Exception ex) {
            LOGGER.error("Stream update failed with exception", ex);
            return false;
        }
        updatedStreamCount.mark();
        return true;
    }

    private boolean unknown(StreamTask task) {
        LOGGER.error("Unknown task type {}", task.getType());
        return true;
    }

    @FunctionalInterface
    private interface StreamTaskProcessor {
        /**
         * Process the task.
         *
         * @param task the task
         * @return {@code true} if task was processed. Return {@code false} if the task should be retried.
         */
        boolean process(StreamTask task);
    }
}
