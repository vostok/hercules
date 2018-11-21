package ru.kontur.vostok.hercules.kafka.util.processing.bulk;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class to store kafka records
 *
 * @param <Key> kafka record key type
 * @param <Value> kafka record value type
 */
public class RecordStorage<Key, Value> {

    private final int storageSize;
    private final List<Value> records;

    private final Map<String, Map<Integer, Long>> offsets = new HashMap<>();

    /**
     * @param storageSize storage can be filled until capacity is exceeded
     */
    public RecordStorage(int storageSize) {
        this.storageSize = storageSize;
        this.records = new ArrayList<>(storageSize);
    }

    /**
     * Add record to storage
     * @param record record
     */
    public void add(ConsumerRecord<Key, Value> record) {
        if (!available()) {
            throw new IllegalStateException("Exceeded capacity");
        }
        records.add(record.value());

        Map<Integer, Long> topicOffsets = offsets.computeIfAbsent(record.topic(), s -> new HashMap<>());
        topicOffsets.put(record.partition(), record.offset());
    }

    /**
     * @return true if capacity is not exceeded else false
     */
    public boolean available() {
        return records.size() < storageSize;
    }

    /**
     * Return map of offsets for records stored in this storage
     * @param metadata metadata will be added to offset record in kafka (might be useful for debugging)
     * @return map of offsets
     */
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

    /**
     * @return get currently stored records list
     */
    public List<Value> getRecords() {
        return records;
    }
}
