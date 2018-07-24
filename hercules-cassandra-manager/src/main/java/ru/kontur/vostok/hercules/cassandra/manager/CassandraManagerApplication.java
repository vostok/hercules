package ru.kontur.vostok.hercules.cassandra.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import ru.kontur.vostok.hercules.cassandra.util.CassandraConnector;
import ru.kontur.vostok.hercules.management.task.cassandra.CassandraTask;
import ru.kontur.vostok.hercules.util.args.ArgsParser;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Gregory Koshelev
 */
public class CassandraManagerApplication {
    private static CassandraConnector cassandraConnector;
    private static ExecutorService executor = Executors.newFixedThreadPool(1);
    private static KafkaConsumer<Void, byte[]> consumer;

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        try {
            Map<String, String> parameters = ArgsParser.parse(args);

            Properties cassandraProperties = PropertiesUtil.readProperties(parameters.getOrDefault("cassandra.properties", "cassandra.properties"));
            Properties consumerProperties = PropertiesUtil.readProperties(parameters.getOrDefault("consumer.properties", "consumer.properties"));

            cassandraConnector = new CassandraConnector(cassandraProperties);
            cassandraConnector.connect();

            final CassandraManager cassandraManager = new CassandraManager(cassandraConnector);

            consumer = new KafkaConsumer<>(consumerProperties);

            final String topic = "hercules_management_kafka";
            ObjectMapper objectMapper = new ObjectMapper();
            final ObjectReader deserializer = objectMapper.readerFor(CassandraTask.class);

            executor.submit(() -> {
                try {
                    consumer.subscribe(Collections.singletonList(topic));

                    while (true) {
                        ConsumerRecords<Void, byte[]> records = consumer.poll(Long.MAX_VALUE);
                        for (ConsumerRecord<Void, byte[]> record : records) {
                            byte[] value = record.value();
                            CassandraTask task;
                            try {
                                task = deserializer.readValue(value);
                            } catch (IOException e) {
                                e.printStackTrace();
                                continue;
                            }

                            switch (task.getType()) {
                                case CREATE:
                                    cassandraManager.createTable(task.getTable());
                                    break;
                                case DELETE:
                                    cassandraManager.deleteTable(task.getTable());
                                    break;
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

        Runtime.getRuntime().addShutdownHook(new Thread(CassandraManagerApplication::shutdown));

        System.out.println("Cassandra Manager started for " + (System.currentTimeMillis() - start) + " millis");
    }

    private static void shutdown() {
        long start = System.currentTimeMillis();
        System.out.println("Started Cassandra Manager shutdown");

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
            if (cassandraConnector != null) {
                cassandraConnector.close();
            }
        } catch (Throwable e) {
            e.printStackTrace();//TODO: Process error
        }

        System.out.println("Finished Cassandra Manager shutdown for " + (System.currentTimeMillis() - start) + " millis");
    }
}
