package ru.kontur.vostok.hercules.cassandra.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.kafka.util.serialization.VoidDeserializer;
import ru.kontur.vostok.hercules.management.task.TaskConstants;
import ru.kontur.vostok.hercules.management.task.cassandra.CassandraTask;

import java.io.IOException;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Gregory Koshelev
 */
public class CassandraTaskConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraTaskConsumer.class);

    private final ExecutorService executor = Executors.newFixedThreadPool(1);
    private final CassandraManager cassandraManager;
    private final KafkaConsumer<Void, byte[]> consumer;

    public CassandraTaskConsumer(Properties properties, CassandraManager cassandraManager) {
        this.cassandraManager = cassandraManager;
        this.consumer = new KafkaConsumer<>(properties, new VoidDeserializer(), new ByteArrayDeserializer());
    }

    public void start() {
        ObjectMapper objectMapper = new ObjectMapper();
        final ObjectReader deserializer = objectMapper.readerFor(CassandraTask.class);

        executor.submit(() -> {
            try {
                consumer.subscribe(Collections.singletonList(TaskConstants.CASSANDRA_TASK_TOPIC));

                while (true) {
                    ConsumerRecords<Void, byte[]> records = consumer.poll(Long.MAX_VALUE);
                    for (ConsumerRecord<Void, byte[]> record : records) {
                        byte[] value = record.value();
                        CassandraTask task;
                        try {
                            task = deserializer.readValue(value);
                        } catch (IOException e) {
                            LOGGER.error("Error reading value", e);
                            continue;
                        }

                        switch (task.getType()) {
                            case CREATE:
                                cassandraManager.createTable(task.getTable());
                                LOGGER.info("Created table '{}'", task.getTable());
                                break;
                            case DELETE:
                                cassandraManager.deleteTable(task.getTable());
                                LOGGER.info("Deleted table '{}'", task.getTable());
                                break;
                        }
                    }
                }
            } catch (WakeupException e) {
                // ignore for shutdown
            } catch (Exception e) {
                LOGGER.error("Error on processing tasks", e);
            } finally {
                consumer.close();
            }
        });
    }

    public void stop() {
        consumer.wakeup();

        executor.shutdown();
    }
}
