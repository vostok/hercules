package ru.kontur.vostok.hercules.management.task.kafka;

/**
 * @author Gregory Koshelev
 */
public class CreateTopicKafkaTask extends KafkaTask {
    private int partitions;

    public CreateTopicKafkaTask(String topic, int partitions) {
        super(topic);
        this.partitions = partitions;
    }

    public int getPartitions() {
        return partitions;
    }
    public void setPartitions(int partitions) {
        this.partitions = partitions;
    }
}
