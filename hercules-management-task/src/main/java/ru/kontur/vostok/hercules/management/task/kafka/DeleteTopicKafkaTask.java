package ru.kontur.vostok.hercules.management.task.kafka;

/**
 * @author Gregory Koshelev
 */
public class DeleteTopicKafkaTask extends KafkaTask {
    public DeleteTopicKafkaTask(String topic) {
        super(topic);
    }
}
