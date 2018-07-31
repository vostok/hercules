package ru.kontur.vostok.hercules.cassandra.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
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
    private final ExecutorService executor = Executors.newFixedThreadPool(1);
    private final CassandraManager cassandraManager;
    private final KafkaConsumer<Void, byte[]> consumer;

    public CassandraTaskConsumer(Properties properties, CassandraManager cassandraManager) {
        this.cassandraManager = cassandraManager;
        this.consumer = new KafkaConsumer<>(properties);
    }

    public void start() {
        ObjectMapper objectMapper = new ObjectMapper();
        final ObjectReader deserializer = objectMapper.readerFor(CassandraTask.class);

        executor.submit(() -> {
            try {
                consumer.subscribe(Collections.singletonList(TaskConstants.cassandraTaskTopic));

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
    }

    public void stop() {
        consumer.wakeup();

        executor.shutdown();
    }
}