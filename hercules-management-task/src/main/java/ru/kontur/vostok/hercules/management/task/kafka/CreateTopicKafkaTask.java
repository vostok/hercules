package ru.kontur.vostok.hercules.management.task.kafka;

/**
 * @author Gregory Koshelev
 */
public class CreateTopicKafkaTask extends KafkaTask {
    private int partitions;
    private Long ttl;

    public CreateTopicKafkaTask(String topic, int partitions, Long ttl) {
        super(topic);
        this.partitions = partitions;
        this.ttl = ttl;
    }

    public CreateTopicKafkaTask() {
    }

    public int getPartitions() {
        return partitions;
    }
    public void setPartitions(int partitions) {
        this.partitions = partitions;
    }

    public Long getTtl() {
        return ttl;
    }
    public void setTtl(Long ttl) {
        this.ttl = ttl;
    }
}
