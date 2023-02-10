package ru.kontur.vostok.hercules.sink.parallel;

import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.sink.ProcessorResult;
import ru.kontur.vostok.hercules.uuid.UuidGenerator;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Events batch with processing statuses
 *
 * @author Innokentiy Krivonosov
 */
public class EventsBatch<T> {
    private final UUID id = UuidGenerator.getClientInstance().next();

    final Map<TopicPartition, OffsetAndMetadata> offsetsToCommit;
    final Map<TopicPartition, List<byte[]>> rawEvents;
    final int rawEventsCount;
    final long rawEventsByteSize;

    public final Map<TopicPartition, List<Event>> events = new HashMap<>();
    private volatile T preparedData = null;
    private volatile ProcessorResult processorResult = null;

    private EventsBatch(
            Map<TopicPartition, OffsetAndMetadata> offsetsToCommit,
            Map<TopicPartition, List<byte[]>> rawEvents,
            int rawEventsCount,
            long rawEventsByteSize
    ) {
        this.offsetsToCommit = offsetsToCommit;
        this.rawEvents = rawEvents;
        this.rawEventsCount = rawEventsCount;
        this.rawEventsByteSize = rawEventsByteSize;
    }

    public T getPreparedData() {
        return preparedData;
    }

    public boolean isReadyToSend() {
        return preparedData != null;
    }

    public void setPreparedData(T preparedData) {
        this.preparedData = preparedData;
    }

    public boolean isProcessFinished() {
        return processorResult != null;
    }

    public void setProcessorResult(ProcessorResult sendResult) {
        this.processorResult = sendResult;
    }

    public ProcessorResult getProcessorResult() {
        return processorResult;
    }

    public List<Event> getAllEvents() {
        return events.values()
                .stream()
                .flatMap(List::stream)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventsBatch<?> that = (EventsBatch<?>) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static class EventsBatchBuilder<T> {
        final Map<TopicPartition, OffsetAndMetadata> offsetsToCommit = new HashMap<>();
        final Map<TopicPartition, List<byte[]>> rawEvents = new HashMap<>();

        int rawEventsCount = 0;
        long rawEventsByteSize = 0;

        public EventsBatch<T> build() {
            return new EventsBatch<>(
                    Collections.unmodifiableMap(offsetsToCommit),
                    Collections.unmodifiableMap(rawEvents),
                    rawEventsCount,
                    rawEventsByteSize
            );
        }
    }
}
