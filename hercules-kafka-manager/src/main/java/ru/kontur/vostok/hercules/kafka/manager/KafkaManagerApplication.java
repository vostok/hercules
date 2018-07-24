package ru.kontur.vostok.hercules.kafka.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import ru.kontur.vostok.hercules.management.task.kafka.CreateTopicKafkaTask;
import ru.kontur.vostok.hercules.management.task.kafka.DeleteTopicKafkaTask;
import ru.kontur.vostok.hercules.management.task.kafka.IncreasePartitionsKafkaTask;
import ru.kontur.vostok.hercules.management.task.kafka.KafkaTask;
import ru.kontur.vostok.hercules.util.args.ArgsParser;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class KafkaManagerApplication {
    private static KafkaManager kafkaManager;
    private static ExecutorService executor = Executors.newFixedThreadPool(1);
    private static KafkaConsumer<Void, byte[]> consumer;

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        try {
            Map<String, String> parameters = ArgsParser.parse(args);

            Properties applicationProperties = PropertiesUtil.readProperties(parameters.getOrDefault("application.properties", "application.properties"));
            Properties kafkaProperties = PropertiesUtil.readProperties(parameters.getOrDefault("kafka.properties", "kafka.properties"));
            Properties consumerProperties = PropertiesUtil.readProperties(parameters.getOrDefault("consumer.properties", "consumer.properties"));

            kafkaManager = new KafkaManager(kafkaProperties, PropertiesUtil.get(applicationProperties, "replicationFactor", (short) 1));

            consumer = new KafkaConsumer<>(consumerProperties);

            final String topic = "hercules_management_kafka";
            ObjectMapper objectMapper = new ObjectMapper();
            final ObjectReader deserializer = objectMapper.readerFor(KafkaTask.class);

            executor.submit(() -> {
                try {
                    consumer.subscribe(Collections.singletonList(topic));

                    while(true) {
                        ConsumerRecords<Void, byte[]> records = consumer.poll(Long.MAX_VALUE);
                        for (ConsumerRecord<Void, byte[]> record : records) {
                            byte[] value = record.value();
                            KafkaTask task;
                            try {
                                task = deserializer.readValue(value);
                            } catch (IOException e) {
                                e.printStackTrace();
                                continue;
                            }
                            if (task instanceof CreateTopicKafkaTask) {
                                CreateTopicKafkaTask createTopicKafkaTask = (CreateTopicKafkaTask) task;
                                kafkaManager.createTopic(createTopicKafkaTask.getTopic(), createTopicKafkaTask.getPartitions());
                                continue;
                            }
                            if (task instanceof DeleteTopicKafkaTask) {
                                kafkaManager.deleteTopic(task.getTopic());
                            }
                            if (task instanceof IncreasePartitionsKafkaTask) {
                                IncreasePartitionsKafkaTask increasePartitionsKafkaTask = (IncreasePartitionsKafkaTask) task;
                                kafkaManager.increasePartitions(increasePartitionsKafkaTask.getTopic(), increasePartitionsKafkaTask.getNewPartitions());
                            }
                        }
                    }
                } catch (WakeupException e) {
                    // ignore for shutdown
                } finally {
                    consumer.close();
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
            shutdown();
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(KafkaManagerApplication::shutdown));

        System.out.println("Cassandra Manager started for " + (System.currentTimeMillis() - start) + " millis");
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
                consumer.wakeup();
            }
        } catch (Throwable e) {
            e.printStackTrace();//TODO: Process error
        }

        try {
            if (executor != null) {
                executor.shutdown();
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

        System.out.println("Finished Cassandra Manager shutdown for " + (System.currentTimeMillis() - start) + " millis");
    }
}
