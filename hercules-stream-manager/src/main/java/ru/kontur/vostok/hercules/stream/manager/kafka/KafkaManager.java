package ru.kontur.vostok.hercules.stream.manager.kafka;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterConfigsResult;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.CreatePartitionsResult;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.apache.kafka.clients.admin.DescribeConfigsResult;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import ru.kontur.vostok.hercules.util.time.DurationUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class KafkaManager {
    private final AdminClient adminClient;
    private final short replicationFactor;

    public KafkaManager(Properties properties, short replicationFactor) {
        this.adminClient = AdminClient.create(properties);
        this.replicationFactor = replicationFactor;
    }

    /**
     * Create topic in Kafka.
     *
     * @param topic topic
     * @return {@link CreateTopicResult#CREATED} if topic has been created, {@link CreateTopicResult#ALREADY_EXISTS} if topic is already exists
     * @throws KafkaManagerException in case of any error
     */
    public CreateTopicResult createTopic(Topic topic) throws KafkaManagerException {
        Map<String, String> configs = new HashMap<>();
        configs.put(TopicConfig.RETENTION_MS_CONFIG, Long.toString(topic.ttl()));
        //TODO: Replicas assignment should be used
        NewTopic newTopic = new NewTopic(topic.name(), topic.partitions(), replicationFactor).configs(configs);

        CreateTopicsResult result = adminClient.createTopics(Collections.singletonList(newTopic));
        Future<Void> future = result.values().get(topic.name());
        try {
            future.get();
            return CreateTopicResult.CREATED;
        } catch (Exception ex) {
            if (ex.getCause() instanceof TopicExistsException) {
                return CreateTopicResult.ALREADY_EXISTS;
            }
            throw new KafkaManagerException(ex);
        }
    }

    /**
     * Delete topic in Kafka.
     *
     * @param topic topic
     * @return {@link DeleteTopicResult#DELETED} if topic has been deleted, {@link DeleteTopicResult#NOT_FOUND} if topic not found (thus, nothing to delete
     * @throws KafkaManagerException
     */
    public DeleteTopicResult deleteTopic(String topic) throws KafkaManagerException {
        DeleteTopicsResult result = adminClient.deleteTopics(Collections.singletonList(topic));
        Future<Void> future = result.values().get(topic);
        try {
            future.get();
            return DeleteTopicResult.DELETED;
        } catch (Exception ex) {
            if (ex.getCause() instanceof UnknownTopicOrPartitionException) {
                return DeleteTopicResult.NOT_FOUND;
            }
            throw new KafkaManagerException(ex);
        }
    }

    /**
     * Add partitions to topic in Kafka.
     * <p>
     * New partition count should be greater than old one.
     *
     * @param topic topic
     * @return {@link UpdateTopicResult#UPDATED} if partitions has been added successfully, {@link UpdateTopicResult#NOT_FOUND} if topic not found
     * @throws KafkaManagerException in case of any error
     */
    public UpdateTopicResult increasePartitions(Topic topic) throws KafkaManagerException {
        NewPartitions request = NewPartitions.increaseTo(topic.partitions());

        CreatePartitionsResult result = adminClient.createPartitions(Collections.singletonMap(topic.name(), request));
        Future<Void> future = result.values().get(topic.name());
        try {
            future.get();
            return UpdateTopicResult.UPDATED;
        } catch (Exception ex) {
            if (ex.getCause() instanceof UnknownTopicOrPartitionException) {
                return UpdateTopicResult.NOT_FOUND;
            }
            throw new KafkaManagerException(ex);
        }
    }

    /**
     * Change TTL (i.e. {@code retention.ms} config) of topic in Kafka.
     *
     * @param topic topic
     * @return {@link UpdateTopicResult#UPDATED} if retention period has been updated successfully, {@link UpdateTopicResult#NOT_FOUND} if topic not found
     * @throws KafkaManagerException in case of any error
     */
    public UpdateTopicResult changeTtl(Topic topic) throws KafkaManagerException {
        ConfigResource resourceConfig = new ConfigResource(ConfigResource.Type.TOPIC, topic.name());
        ConfigEntry retentionConfigEntry = new ConfigEntry(TopicConfig.RETENTION_MS_CONFIG, Long.toString(topic.ttl()));
        Map<ConfigResource, Config> updateConfig = new HashMap<>();
        updateConfig.put(resourceConfig, new Config(Collections.singleton(retentionConfigEntry)));

        AlterConfigsResult result = adminClient.alterConfigs(updateConfig);
        Future<Void> future = result.values().get(resourceConfig);
        try {
            future.get();
            return UpdateTopicResult.UPDATED;
        } catch (Exception ex) {
            // Actually, never happen. See https://issues.apache.org/jira/browse/KAFKA-10033
            if (ex.getCause() instanceof UnknownTopicOrPartitionException) {
                return UpdateTopicResult.NOT_FOUND;
            }
            throw new KafkaManagerException(ex);
        }
    }

    /**
     * Get topic from Kafka.
     *
     * @param topic topic name
     * @return topic if it exists or {@link Optional#empty()} otherwise
     * @throws KafkaManagerException in case of any error
     */
    public Optional<Topic> getTopic(String topic) throws KafkaManagerException {
        try {
            int partitions = getPartitions(topic);
            long ttl = getTtl(topic);
            return Optional.of(new Topic(topic, partitions, ttl));
        } catch (TopicNotFoundException ex) {
            return Optional.empty();
        }
    }

    public void close(long duration, TimeUnit unit) {
        adminClient.close(DurationUtil.of(duration, unit));
    }

    /**
     * Get partition count for topic.
     *
     * @param topic topic
     * @return partition count
     * @throws TopicNotFoundException if Topic not found
     * @throws KafkaManagerException  in case of any error
     */
    private int getPartitions(String topic) throws TopicNotFoundException, KafkaManagerException {
        DescribeTopicsResult result = adminClient.describeTopics(Collections.singletonList(topic));
        KafkaFuture<TopicDescription> future = result.values().get(topic);
        try {
            TopicDescription topicDescription = future.get();
            return topicDescription.partitions().size();
        } catch (Exception ex) {
            if (ex.getCause() instanceof UnknownTopicOrPartitionException) {
                throw new TopicNotFoundException();
            }
            throw new KafkaManagerException(ex);
        }
    }

    /**
     * Get TTL (i.e. {@code retention.ms} config) of topic.
     *
     * @param topic topic
     * @return TTL if defined, otherwise {@code -1}
     * @throws TopicNotFoundException if Topic not found
     * @throws KafkaManagerException  in case of any error
     */
    private long getTtl(String topic) throws TopicNotFoundException, KafkaManagerException {
        ConfigResource configResource = new ConfigResource(ConfigResource.Type.TOPIC, topic);
        DescribeConfigsResult result = adminClient.describeConfigs(Collections.singletonList(configResource));
        KafkaFuture<Config> future = result.values().get(configResource);
        try {
            Config config = future.get();
            ConfigEntry configEntry = config.get(TopicConfig.RETENTION_MS_CONFIG);
            return (configEntry != null) ? Long.valueOf(configEntry.value()) : -1;
        } catch (Exception ex) {
            if (ex.getCause() instanceof UnknownTopicOrPartitionException) {
                throw new TopicNotFoundException();
            }
            throw new KafkaManagerException(ex);
        }
    }
}
