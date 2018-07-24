package ru.kontur.vostok.hercules.management.task.kafka;

/**
 * @author Gregory Koshelev
 */
public class IncreasePartitionsKafkaTask extends KafkaTask {
    private int newPartitions;

    public IncreasePartitionsKafkaTask(String topic, int newPartitions) {
        super(topic);
        this.newPartitions = newPartitions;
    }

    public int getNewPartitions() {
        return newPartitions;
    }
    public void setNewPartitions(int newPartitions) {
        this.newPartitions = newPartitions;
    }
}
