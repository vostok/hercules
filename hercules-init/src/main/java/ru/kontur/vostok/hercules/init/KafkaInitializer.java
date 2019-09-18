package ru.kontur.vostok.hercules.init;

import org.apache.kafka.clients.admin.AdminClient;
import ru.kontur.vostok.hercules.kafka.util.KafkaDefaults;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.validation.IntegerValidators;

import java.util.Properties;

/**
 * @author Gregory Koshelev
 */
public class KafkaInitializer {

    private final Properties kafkaProperties;
    private final int replicationFactor;

    public KafkaInitializer(Properties properties) {
        this.kafkaProperties = properties;
        this.replicationFactor = PropertiesUtil.get(Props.REPLICATION_FACTOR, properties).get();
    }

    public void init() {
        try (AdminClient adminClient = AdminClient.create(kafkaProperties)) {
        }
    }

    private static class Props {
        static final Parameter<Integer> REPLICATION_FACTOR =
                Parameter.integerParameter("replication.factor").
                        withDefault(KafkaDefaults.DEFAULT_REPLICATION_FACTOR).
                        withValidator(IntegerValidators.positive()).
                        build();
    }
}
