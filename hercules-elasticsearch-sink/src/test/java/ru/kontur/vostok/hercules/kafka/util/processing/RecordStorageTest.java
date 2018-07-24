package ru.kontur.vostok.hercules.kafka.util.processing;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class RecordStorageTest {

    @Test(expected = IllegalStateException.class)
    public void shouldThrowOnSctorageCapacityExceeded() throws Exception {
        RecordStorage<String, String> storage = new RecordStorage<>(1);
        storage.add(createRecord("k", "v1"));
        storage.add(createRecord("k", "v2"));
    }

    @Test
    public void shuldReturnTrueIfHasAvailablePlace() throws Exception {
        RecordStorage<String, String> storage = new RecordStorage<>(2);
        storage.add(createRecord("k", "v"));

        Assert.assertTrue(storage.available());
    }

    @Test
    public void shuldReturnFalseIfNoAvailablePlace() throws Exception {
        RecordStorage<String, String> storage = new RecordStorage<>(1);
        storage.add(createRecord("k", "v"));

        Assert.assertFalse(storage.available());
    }

    @Test
    public void shouldReturnCorrectOffsetMap() throws Exception {
        RecordStorage<String, String> storage = new RecordStorage<>(10);

        storage.add(new ConsumerRecord<>("topic-1", 0, 123, "k", "v"));
        storage.add(new ConsumerRecord<>("topic-2", 0, 234, "k", "v"));
        storage.add(new ConsumerRecord<>("topic-1", 1, 456, "k", "v"));
        storage.add(new ConsumerRecord<>("topic-2", 0, 235, "k", "v"));


        Map<TopicPartition, OffsetAndMetadata> offsetMap = storage.getOffsets("metadata");

        Assert.assertEquals(3, offsetMap.size());
        Assert.assertEquals(new OffsetAndMetadata(123, "metadata"), offsetMap.get(new TopicPartition("topic-1", 0)));
        Assert.assertEquals(new OffsetAndMetadata(456, "metadata"), offsetMap.get(new TopicPartition("topic-1", 1)));
        Assert.assertEquals(new OffsetAndMetadata(235, "metadata"), offsetMap.get(new TopicPartition("topic-2", 0)));
    }

    private static ConsumerRecord<String, String> createRecord(String key, String value) {
        return new ConsumerRecord<>(
                "dummy-topic",
                0,
                0,
                key,
                value
        );
    }
}
