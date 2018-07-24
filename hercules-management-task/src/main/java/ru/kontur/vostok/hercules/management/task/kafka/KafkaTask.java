package ru.kontur.vostok.hercules.management.task.kafka;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * @author Gregory Koshelev
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CreateTopicKafkaTask.class, name = "CREATE"),
        @JsonSubTypes.Type(value = IncreasePartitionsKafkaTask.class, name = "INCREASE_PARTITIONS"),
        @JsonSubTypes.Type(value = DeleteTopicKafkaTask.class, name = "DELETE")
})
public abstract class KafkaTask {
    private String topic;

    public KafkaTask(String topic) {
        this.topic = topic;
    }

    public String getTopic() {
        return topic;
    }
    public void setTopic(String topic) {
        this.topic = topic;
    }
}
