package ru.kontur.vostok.hercules.sink.parallel;

import org.apache.kafka.common.TopicPartition;

import java.util.List;
import java.util.function.Predicate;

/**
 * Event Batch Strategy
 *
 * @author Innokentiy Krivonosov
 */
public interface CreateEventsBatchStrategy<T> {
    List<EventsBatch<T>> create(Predicate<TopicPartition> isAllowedPartition);
}
