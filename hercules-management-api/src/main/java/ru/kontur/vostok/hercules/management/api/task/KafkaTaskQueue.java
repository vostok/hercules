package ru.kontur.vostok.hercules.management.api.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.kafka.util.serialization.VoidSerializer;
import ru.kontur.vostok.hercules.management.task.TaskConstants;
import ru.kontur.vostok.hercules.management.task.kafka.CreateTopicKafkaTask;
import ru.kontur.vostok.hercules.management.task.kafka.DeleteTopicKafkaTask;
import ru.kontur.vostok.hercules.management.task.kafka.IncreasePartitionsKafkaTask;
import ru.kontur.vostok.hercules.management.task.kafka.KafkaTask;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Gregory Koshelev
 */
public class KafkaTaskQueue {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaTaskQueue.class);

    private final KafkaProducer<Void, byte[]> producer;
    private final ObjectWriter serializer;

    public KafkaTaskQueue(Properties properties) {
        this.producer = new KafkaProducer<>(properties, new VoidSerializer(), new ByteArraySerializer());

        ObjectMapper objectMapper = new ObjectMapper();
        this.serializer = objectMapper.writerFor(KafkaTask.class);
    }

    public void createTopic(String topic, int partitions, long ttl) {
        sendTask(new CreateTopicKafkaTask(topic, partitions, ttl));
    }

    public void increasePartitions(String topic, int newPartitions) {
        sendTask(new IncreasePartitionsKafkaTask(topic, newPartitions));
    }

    public void deleteTopic(String topic) {
        sendTask(new DeleteTopicKafkaTask(topic));
    }

    public void close(long timeout, TimeUnit unit) {
        producer.close(timeout, unit);
    }

    private void sendTask(KafkaTask task) {
        try {
            byte[] value = serializer.writeValueAsBytes(task);
            ProducerRecord<Void, byte[]> record = new ProducerRecord<>(TaskConstants.kafkaTaskTopic, value);
            Future<RecordMetadata> result = producer.send(record);
            result.get(5_000, TimeUnit.MILLISECONDS);
        } catch (JsonProcessingException | InterruptedException | ExecutionException | TimeoutException e) {
            LOGGER.error("Error on sending task", e);
            throw new RuntimeException("Cannot send task", e);
        }
    }
}
