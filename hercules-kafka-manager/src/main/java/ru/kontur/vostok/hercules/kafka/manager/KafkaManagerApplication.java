package ru.kontur.vostok.hercules.kafka.manager;

import ru.kontur.vostok.hercules.configuration.util.ArgsParser;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class KafkaManagerApplication {
    private static KafkaManager kafkaManager;
    private static KafkaTaskConsumer consumer;

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        try {
            Map<String, String> parameters = ArgsParser.parse(args);

            Properties applicationProperties = PropertiesUtil.readProperties(parameters.getOrDefault("application.properties", "application.properties"));
            Properties kafkaProperties = PropertiesUtil.readProperties(parameters.getOrDefault("kafka.properties", "kafka.properties"));
            Properties consumerProperties = PropertiesUtil.readProperties(parameters.getOrDefault("consumer.properties", "consumer.properties"));

            kafkaManager = new KafkaManager(kafkaProperties, PropertiesUtil.getShort(applicationProperties, "replicationFactor", (short) 1));

            consumer = new KafkaTaskConsumer(consumerProperties, kafkaManager);
            consumer.start();
        } catch (Throwable e) {
            e.printStackTrace();
            shutdown();
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(KafkaManagerApplication::shutdown));

        System.out.println("Kafka Manager started for " + (System.currentTimeMillis() - start) + " millis");
    }

    private static void shutdown() {
        long start = System.currentTimeMillis();
        System.out.println("Started Cassandra Manager shutdown");
        try {
            if (kafkaManager != null) {
                kafkaManager.close(5_000, TimeUnit.MILLISECONDS);
            }
        } catch (Throwable e) {
            e.printStackTrace();//TODO: Process error
        }

        try {
            if (consumer != null) {
                consumer.stop();
            }
        } catch (Throwable e) {
            e.printStackTrace();//TODO: Process error
        }

        try {
            if (kafkaManager != null) {
                kafkaManager.close(5_000, TimeUnit.MILLISECONDS);
            }
        } catch (Throwable e) {
            e.printStackTrace();//TODO: Process error
        }

        System.out.println("Finished Kafka Manager shutdown for " + (System.currentTimeMillis() - start) + " millis");
    }
}
