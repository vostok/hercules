package ru.kontur.vostok.hercules.sink.parallel;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.jetbrains.annotations.NotNull;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventBuilder;

import java.util.List;
import java.util.UUID;

/**
 * @author Innokentiy Krivonosov
 */
public class TestUtils {
    public static final String TOPIC = "topic";
    private static int offset = 0;

    public static Event createEvent() {
        return EventBuilder.create().timestamp(1).uuid(UUID.randomUUID()).build();
    }

    static ConsumerRecord<byte[], byte[]> record(TopicPartition topicPartition, Event event) {
        return new ConsumerRecord<>(topicPartition.topic(), topicPartition.partition(), offset++, new byte[0], event.getBytes());
    }

    public static ConsumerRecord<UUID, Event> eventRecord(TopicPartition topicPartition, Event event) {
        return new ConsumerRecord<>(topicPartition.topic(), topicPartition.partition(), offset++, UUID.randomUUID(), event);
    }

    @NotNull
    static EventsBatch<TestPreparedData> getEventsBatch(TopicPartition... topicPartitions) {
        EventsBatch.EventsBatchBuilder<TestPreparedData> eventsBatch = new EventsBatch.EventsBatchBuilder<>();

        for (TopicPartition topicPartition : topicPartitions) {
            Event event = createEvent();
            eventsBatch.rawEvents.put(topicPartition, List.of(event.getBytes()));
            eventsBatch.offsetsToCommit.put(topicPartition, new OffsetAndMetadata(10));
            eventsBatch.rawEventsCount++;
            eventsBatch.rawEventsByteSize = event.sizeOf();
        }

        return eventsBatch.build();
    }
}
