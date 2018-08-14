package ru.kontur.vostok.hercules.init;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import ru.kontur.vostok.hercules.kafka.util.KafkaDefaults;
import ru.kontur.vostok.hercules.management.task.TaskConstants;
import ru.kontur.vostok.hercules.util.properties.PropertiesExtractor;

import java.util.Arrays;
import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class KafkaInitializer {
    private final Properties kafkaProperties;

    public KafkaInitializer(Properties properties) {
        this.kafkaProperties = properties;
    }

    public void init() {
        final short replicationFactor = PropertiesExtractor.getShort(kafkaProperties, "replication.factor", KafkaDefaults.DEFAULT_REPLICATION_FACTOR);
        try (AdminClient adminClient = AdminClient.create(kafkaProperties)) {
            CreateTopicsResult result = adminClient.createTopics(
                    Arrays.asList(
                            new NewTopic(TaskConstants.kafkaTaskTopic, 1, replicationFactor),
                            new NewTopic(TaskConstants.cassandraTaskTopic, 1, replicationFactor)));
            result.all();
        }
    }
}
