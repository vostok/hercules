package ru.kontur.vostok.hercules.kafka.manager;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreatePartitionsResult;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class KafkaManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaManager.class);

    private final AdminClient adminClient;
    private final short replicationFactor;

    public KafkaManager(Properties properties, short replicationFactor) {
        this.adminClient = AdminClient.create(properties);
        this.replicationFactor = replicationFactor;
    }

    public void createTopic(String topic, int partitions) {
        NewTopic newTopic = new NewTopic(topic, partitions, replicationFactor);//TODO: Replicas assignment should be used
        CreateTopicsResult result = adminClient.createTopics(Collections.singletonList(newTopic));
        Future<Void> future = result.values().get(topic);
        try {
            future.get();
        } catch (Exception e) {
            throw new RuntimeException("Topic creation fails with exception", e);
        }
    }

    public void increasePartitions(String topic, int newPartitions) {
        NewPartitions request = NewPartitions.increaseTo(newPartitions);
        CreatePartitionsResult result = adminClient.createPartitions(Collections.singletonMap(topic, request));
        Future<Void> future = result.values().get(topic);
        try {
            future.get();
        } catch (Exception e) {
            throw new RuntimeException("Increasing of partitions count fails with exception", e);
        }
    }

    public void deleteTopic(String topic) {
        DeleteTopicsResult result = adminClient.deleteTopics(Collections.singletonList(topic));
        Future<Void> future = result.all();
        try {
            future.get();
        } catch (Exception e) {
            throw new RuntimeException("Topic deletion fails with exception", e);
        }
    }

    public void close(long duration, TimeUnit unit) {
        adminClient.close(duration, unit);
    }
}
