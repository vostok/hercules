package ru.kontur.vostok.hercules.init;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import ru.kontur.vostok.hercules.management.task.TaskConstants;

import java.util.Arrays;
import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class KafkaInitializer {
    private final Properties kafkaProperties;
    private final short replicationFactor;

    public KafkaInitializer(Properties properties, short replicationFactor) {
        this.kafkaProperties = properties;
        this.replicationFactor = replicationFactor;
    }

    public void init() {
        try (AdminClient adminClient = AdminClient.create(kafkaProperties)) {
            CreateTopicsResult result = adminClient.createTopics(
                    Arrays.asList(
                            new NewTopic(TaskConstants.kafkaTaskTopic, 1, replicationFactor),
                            new NewTopic(TaskConstants.cassandraTaskTopic, 1, replicationFactor)));
            result.all();
        }
    }
}
