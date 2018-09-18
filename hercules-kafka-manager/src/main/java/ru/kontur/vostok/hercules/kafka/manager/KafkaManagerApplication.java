package ru.kontur.vostok.hercules.kafka.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.configuration.util.ArgsParser;
import ru.kontur.vostok.hercules.configuration.util.PropertiesReader;
import ru.kontur.vostok.hercules.configuration.util.PropertiesUtil;
import ru.kontur.vostok.hercules.util.properties.PropertiesExtractor;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class KafkaManagerApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaManagerApplication.class);

    private static KafkaManager kafkaManager;
    private static KafkaTaskConsumer consumer;

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        try {
            Map<String, String> parameters = ArgsParser.parse(args);

            Properties properties = PropertiesReader.read(parameters.getOrDefault("application.properties", "application.properties"));
            Properties kafkaProperties = PropertiesUtil.ofScope(properties, Scopes.KAFKA);
            Properties consumerProperties = PropertiesUtil.ofScope(properties, Scopes.CONSUMER);

            kafkaManager = new KafkaManager(kafkaProperties, PropertiesExtractor.getShort(properties, "replicationFactor", (short) 1));

            consumer = new KafkaTaskConsumer(consumerProperties, kafkaManager);
            consumer.start();
        } catch (Throwable t) {
            LOGGER.error("Error on starting kafka manager", t);
            shutdown();
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(KafkaManagerApplication::shutdown));

        LOGGER.info("Kafka Manager started for {} millis", System.currentTimeMillis() - start);
    }

    private static void shutdown() {
        long start = System.currentTimeMillis();
        LOGGER.info("Started Cassandra Manager shutdown");
        try {
            if (kafkaManager != null) {
                kafkaManager.close(5_000, TimeUnit.MILLISECONDS);
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping kafka manager", t);
            //TODO: Process error
        }

        try {
            if (consumer != null) {
                consumer.stop();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping consumer", t);
            //TODO: Process error
        }

        LOGGER.info("Finished Kafka Manager shutdown for {} millis", System.currentTimeMillis() - start);
    }
}
