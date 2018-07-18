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
    private final Map<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();
    private final List<Value> records;

    public RecordStorage(int storageSize) {
        this.storageSize = storageSize;
        this.records = new ArrayList<>(storageSize);
    }

    public void add(ConsumerRecord<Key, Value> record) {
        if (!available()) {
            throw new IllegalStateException("Exceeded capacity");
        }
        records.add(record.value());
        offsets.put(
                new TopicPartition(record.topic(), record.partition()),
                new OffsetAndMetadata(record.offset())
        );

    }

    public boolean available() {
        return records.size() < storageSize;
    }

    public Map<TopicPartition, OffsetAndMetadata> getOffsets() {
        return offsets;
    }

    public List<Value> getRecords() {
        return records;
    }
}
