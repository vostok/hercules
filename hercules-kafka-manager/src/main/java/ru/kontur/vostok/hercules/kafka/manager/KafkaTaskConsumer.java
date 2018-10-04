package ru.kontur.vostok.hercules.kafka.manager;

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
import ru.kontur.vostok.hercules.management.task.kafka.CreateTopicKafkaTask;
import ru.kontur.vostok.hercules.management.task.kafka.DeleteTopicKafkaTask;
import ru.kontur.vostok.hercules.management.task.kafka.IncreasePartitionsKafkaTask;
import ru.kontur.vostok.hercules.management.task.kafka.KafkaTask;

import java.io.IOException;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Gregory Koshelev
 */
public class KafkaTaskConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaTaskConsumer.class);

    private final ExecutorService executor = Executors.newFixedThreadPool(1);
    private final KafkaManager kafkaManager;
    private final KafkaConsumer<Void, byte[]> consumer;

    public KafkaTaskConsumer(Properties properties, KafkaManager kafkaManager) {
        this.kafkaManager = kafkaManager;
        this.consumer = new KafkaConsumer<>(properties, new VoidDeserializer(), new ByteArrayDeserializer());
    }

    public void start() {
        ObjectMapper objectMapper = new ObjectMapper();
        final ObjectReader deserializer = objectMapper.readerFor(KafkaTask.class);

        executor.submit(() -> {
            try {
                consumer.subscribe(Collections.singletonList(TaskConstants.kafkaTaskTopic));

                while(true) {
                    ConsumerRecords<Void, byte[]> records = consumer.poll(Long.MAX_VALUE);
                    for (ConsumerRecord<Void, byte[]> record : records) {
                        byte[] value = record.value();
                        KafkaTask task;
                        try {
                            task = deserializer.readValue(value);
                        } catch (IOException e) {
                            LOGGER.error("Error on message deserialization", e);
                            continue;
                        }
                        if (task instanceof CreateTopicKafkaTask) {
                            CreateTopicKafkaTask createTopicKafkaTask = (CreateTopicKafkaTask) task;
                            kafkaManager.createTopic(createTopicKafkaTask.getTopic(), createTopicKafkaTask.getPartitions(), createTopicKafkaTask.getTtl());
                            continue;
                        }
                        if (task instanceof DeleteTopicKafkaTask) {
                            kafkaManager.deleteTopic(task.getTopic());
                            continue;
                        }
                        if (task instanceof IncreasePartitionsKafkaTask) {
                            IncreasePartitionsKafkaTask increasePartitionsKafkaTask = (IncreasePartitionsKafkaTask) task;
                            kafkaManager.increasePartitions(increasePartitionsKafkaTask.getTopic(), increasePartitionsKafkaTask.getNewPartitions());
                            continue;
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
