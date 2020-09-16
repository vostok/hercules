package ru.kontur.vostok.hercules.stream.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import ru.kontur.vostok.hercules.health.Counter;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.meta.stream.Stream;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.meta.task.TaskExecutor;
import ru.kontur.vostok.hercules.meta.task.stream.StreamTask;
import ru.kontur.vostok.hercules.meta.task.stream.StreamTaskRepository;
import ru.kontur.vostok.hercules.meta.task.stream.StreamTaskType;
import ru.kontur.vostok.hercules.stream.manager.kafka.CreateTopicResult;
import ru.kontur.vostok.hercules.stream.manager.kafka.DeleteTopicResult;
import ru.kontur.vostok.hercules.stream.manager.kafka.KafkaManager;
import ru.kontur.vostok.hercules.stream.manager.kafka.KafkaManagerException;
import ru.kontur.vostok.hercules.stream.manager.kafka.Topic;
import ru.kontur.vostok.hercules.stream.manager.kafka.UpdateTopicResult;
import ru.kontur.vostok.hercules.util.time.TimeSource;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author Gregory Koshelev
 */
public class StreamTaskExecutor extends TaskExecutor<StreamTask> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamTaskExecutor.class);

    private final KafkaManager kafkaManager;
    private final StreamRepository streamRepository;
    private final Counter createdStreamCount;
    private final Counter deletedStreamCount;
    private final Counter updatedStreamCount;

    private final Map<StreamTaskType, Processor> processors;

    protected StreamTaskExecutor(
            StreamTaskRepository streamTaskRepository,
            long pollingTimeoutMillis,
            KafkaManager kafkaManager,
            StreamRepository streamRepository,
            MetricsCollector metricsCollector) {
        super(streamTaskRepository, pollingTimeoutMillis);
        this.kafkaManager = kafkaManager;
        this.streamRepository = streamRepository;
        this.createdStreamCount = metricsCollector.counter("createdStreamCount");
        this.deletedStreamCount = metricsCollector.counter("deletedStreamCount");
        this.updatedStreamCount = metricsCollector.counter("updatedStreamCount");

        Map<StreamTaskType, Processor> processors = new EnumMap<>(StreamTaskType.class);
        processors.put(StreamTaskType.CREATE, this::create);
        processors.put(StreamTaskType.DELETE, this::delete);
        processors.put(StreamTaskType.INCREASE_PARTITIONS, this::increasePartitions);
        processors.put(StreamTaskType.CHANGE_TTL, this::changeTtl);
        processors.put(StreamTaskType.CHANGE_DESCRIPTION, this::tryUpdateStream);
        this.processors = processors;
    }

    @Override
    protected boolean execute(StreamTask task) {
        Stream stream = task.getStream();
        try {
            MDC.put("stream", stream.getName());
            Processor processor = processors.get(task.getType());
            if (processor != null) {
                return processor.process(stream);
            } else {
                LOGGER.error("Unknown task type '{}'", task.getType());
                return true;
            }
        } finally {
            MDC.remove("stream");
        }
    }

    private boolean create(Stream stream) {
        return tryCreateTopic(Topic.forStream(stream)) && tryCreateStream(stream);
    }

    private boolean delete(Stream stream) {
        return tryDeleteStream(stream) && tryDeleteTopic(Topic.forStream(stream));
    }

    private boolean increasePartitions(Stream stream) {
        return tryIncreasePartitionsTopic(Topic.forStream(stream)) && tryUpdateStream(stream);
    }

    private boolean changeTtl(Stream stream) {
        return tryChangeTtlTopic(Topic.forStream(stream)) && tryUpdateStream(stream);
    }

    /**
     * Try to create Stream.
     * <p>
     * Ignore already existed streams. This means that two cases are equivalent:    <br>
     * 1. Successfully created stream.                                              <br>
     * 2. Stream already exists.
     *
     * @param stream stream
     * @return {@code true} if stream has been created (or exists), {@code false} in case of any errors
     */
    private boolean tryCreateStream(Stream stream) {
        try {
            if (streamRepository.create(stream).isSuccess()) {
                LOGGER.info("Stream has been created");
                createdStreamCount.increment();
            } else {
                // Should never happen
                LOGGER.warn("Stream already exists");
            }
        } catch (Exception ex) {
            LOGGER.error("Stream creation failed with exception", ex);
            return false;
        }
        return true;
    }

    /**
     * Try to delete Stream.
     * <p>
     * Ignore not existed streams. This means that two cases are equivalent:    <br>
     * 1. Successfully deleted stream.                                          <br>
     * 2. Stream doesn't exist.
     *
     * @param stream stream
     * @return {@code true} if stream doesn't exist anymore, {@code false} in case of any error
     */
    private boolean tryDeleteStream(Stream stream) {
        try {
            if (streamRepository.delete(stream.getName()).isSuccess()) {
                LOGGER.info("Stream has been deleted");
                deletedStreamCount.increment();
                // FIXME: Workaround for KAFKA-3450 bug (see https://issues.apache.org/jira/browse/KAFKA-3450)
                // If stream has been deleted then should wait until cache of streams in the Gate to be invalidated.
                TimeSource.SYSTEM.sleep(12_000);
            } else {
                // Case is possible only on retry
                LOGGER.warn("Stream does not exist");
            }
        } catch (Exception ex) {
            LOGGER.error("Stream deletion failed with exception", ex);
            return false;
        }
        return true;
    }

    /**
     * Try to update Stream.
     * <p>
     * Ignore not existed streams.
     *
     * @param stream stream
     * @return {@code false} in case of any error, otherwise {@code true}
     */
    private boolean tryUpdateStream(Stream stream) {
        try {
            if (streamRepository.update(stream).isSuccess()) {
                LOGGER.info("Stream has been updated");
                updatedStreamCount.increment();
            } else {
                // Should never happen
                LOGGER.warn("Stream does not exist");
            }
        } catch (Exception ex) {
            LOGGER.error("Stream updating failed with exception", ex);
            return false;
        }
        return true;
    }

    /**
     * Try to create Topic.
     * <p>
     * Ignore already existed topics.
     *
     * @param topic topic
     * @return {@code true} if topic has been created (or exists), {@code false} in case of any errors
     */
    private boolean tryCreateTopic(Topic topic) {
        CreateTopicResult result;
        try {
            result = kafkaManager.createTopic(topic);
            switch (result) {
                case CREATED:
                    LOGGER.info("Topic '{}' has been created: {}", topic.name(), topic);
                    //FIXME: This workaround should be removed when Hercules Gate gets cached topics (see https://issues.apache.org/jira/browse/KAFKA-3450)
                    // After topic creation has a small time interval when brokers don't have newly created topic in their metadata.
                    // Thus, producing fails and gets retries until topic metadata gets up to date
                    TimeSource.SYSTEM.sleep(1_000);
                    return true;
                case ALREADY_EXISTS:
                    // Case is possible only on retry
                    LOGGER.warn("Topic '{}' already exists: need {}, but got {}", topic.name(), topic, kafkaManager.getTopic(topic.name()));
                    return true;
                default:
                    // Should never happen
                    LOGGER.error("Unknown result '{}'", result);
                    return false;
            }
        } catch (KafkaManagerException ex) {
            LOGGER.error("Topic creation failed with exception", ex);
            return false;
        }
    }

    /**
     * Try to delete Topic.
     * <p>
     * Ignore not existed topics. This means that two cases are equivalent: <br>
     * 1. Successfully deleted topic.                                       <br>
     * 2. Topic doesn't exist.
     *
     * @param topic topic
     * @return {@code true} if topic doesn't exist anymore, {@code false} in case of any error
     */
    private boolean tryDeleteTopic(Topic topic) {
        try {
            DeleteTopicResult result = kafkaManager.deleteTopic(topic.name());
            switch (result) {
                case NOT_FOUND:
                    LOGGER.warn("Topic '{}' not found", topic.name());
                    return true;
                case DELETED:
                    LOGGER.info("Topic '{}' has been deleted", topic.name());
                    return true;
                default:
                    LOGGER.error("Unknown result '{}'", result);
                    return false;
            }
        } catch (KafkaManagerException ex) {
            LOGGER.error("Topic deletion failed with exception", ex);
            return false;
        }
    }

    /**
     * Try to increase partitions for topic.
     * <p>
     * Ignore not existed topics.
     *
     * @param topic topic
     * @return {@code false} in case of any errors, otherwise {@code true}
     */
    private boolean tryIncreasePartitionsTopic(Topic topic) {
        try {
            Optional<Topic> actualTopic = kafkaManager.getTopic(topic.name());
            if (!actualTopic.isPresent()) {
                // Should never happen
                LOGGER.warn("Increasing partitions of not existing Topic '{}'", topic.name());
                return true;
            }
            if ((topic.partitions() > actualTopic.get().partitions())) {
                UpdateTopicResult result = kafkaManager.increasePartitions(topic);
                switch (result) {
                    case UPDATED:
                        LOGGER.info("Increased partitions for topic '{}'", topic.name());
                        return true;
                    case NOT_FOUND:
                        // Should never happen
                        LOGGER.warn("Topic '{}' not found", topic.name());
                        return true;
                    default:
                        // Should never happen
                        LOGGER.error("Unknown result '{}'", result);
                        return false;
                }
            }
            return true;
        } catch (KafkaManagerException ex) {
            LOGGER.error("Increasing partitions failed with exception", ex);
            return false;// Task should be retried.
        }
    }

    /**
     * Try to change TTL for Topic.
     * <p>
     * Ignore not existed topics.
     *
     * @param topic topic
     * @return {@code false} in case of any errors, otherwise {@code true}
     */
    private boolean tryChangeTtlTopic(Topic topic) {
        try {
            Optional<Topic> actualTopic = kafkaManager.getTopic(topic.name());
            if (!actualTopic.isPresent()) {
                // Should never happen
                LOGGER.warn("Changing TTL of not existing Topic '{}'", topic.name());
                return true;
            }

            UpdateTopicResult result = kafkaManager.changeTtl(topic);
            switch (result) {
                case UPDATED:
                    LOGGER.info("TTL for Topic '{}' has been changed", topic.name());
                    return true;
                case NOT_FOUND:
                    // Should never happen
                    LOGGER.warn("Topic '{}' not found", topic.name());
                    return true;
                default:
                    // Should never happen
                    LOGGER.error("Unknown result '{}'", result);
                    return false;
            }
        } catch (KafkaManagerException ex) {
            LOGGER.error("Changing Topic TTL failed with exception", ex);
            return false;
        }
    }

    @FunctionalInterface
    private interface Processor {
        /**
         * Process the stream.
         *
         * @param stream the stream
         * @return {@code true} if processed. Return {@code false} if the executor's task should be retried.
         */
        boolean process(Stream stream);
    }
}
