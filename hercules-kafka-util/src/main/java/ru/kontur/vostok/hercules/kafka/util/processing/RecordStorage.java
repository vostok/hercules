package ru.kontur.vostok.hercules.kafka.util.processing;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecordStorage<Key, Value> {

    private final int storageSize;
    private final List<Value> records;

    private final Map<String, Map<Integer, Long>> offsets = new HashMap<>();

    public RecordStorage(int storageSize) {
        this.storageSize = storageSize;
        this.records = new ArrayList<>(storageSize);
    }

    public void add(ConsumerRecord<Key, Value> record) {
        if (!available()) {
            throw new IllegalStateException("Exceeded capacity");
        }
        records.add(record.value());

        Map<Integer, Long> topicOffsets = offsets.computeIfAbsent(record.topic(), s -> new HashMap<>());
        topicOffsets.put(record.partition(), record.offset());
    }

    public boolean available() {
        return records.size() < storageSize;
    }

    public Map<TopicPartition, OffsetAndMetadata> getOffsets(String metadata) {
        Map<TopicPartition, OffsetAndMetadata> result = new HashMap<>();

        for (Map.Entry<String, Map<Integer, Long>> entry : offsets.entrySet()) {
            String topic = entry.getKey();
            for (Map.Entry<Integer, Long> offsetEntry : entry.getValue().entrySet()) {
                Integer partition = offsetEntry.getKey();
                Long offset = offsetEntry.getValue();
                result.put(
                        new TopicPartition(topic, partition),
                        new OffsetAndMetadata(offset, metadata)
                );
            }
        }

        return result;
    }

    public List<Value> getRecords() {
        return records;
    }
}
