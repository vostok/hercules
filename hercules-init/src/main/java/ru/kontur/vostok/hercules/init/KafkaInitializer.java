package ru.kontur.vostok.hercules.init;

import org.apache.kafka.clients.admin.AdminClient;
import ru.kontur.vostok.hercules.kafka.util.KafkaDefaults;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;
import ru.kontur.vostok.hercules.util.validation.IntegerValidators;

import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class KafkaInitializer {

    private static class Props {
        static final PropertyDescription<Integer> REPLICATION_FACTOR = PropertyDescriptions
                .integerProperty("replication.factor")
                .withDefaultValue(KafkaDefaults.DEFAULT_REPLICATION_FACTOR)
                .withValidator(IntegerValidators.positive())
                .build();
    }

    private final Properties kafkaProperties;
    private final int replicationFactor;

    public KafkaInitializer(Properties properties) {
        this.kafkaProperties = properties;
        this.replicationFactor = Props.REPLICATION_FACTOR.extract(properties);
    }

    public void init() {
        try (AdminClient adminClient = AdminClient.create(kafkaProperties)) {
        }
    }
}
