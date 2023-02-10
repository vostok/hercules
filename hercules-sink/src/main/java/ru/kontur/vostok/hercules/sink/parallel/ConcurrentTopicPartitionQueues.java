package ru.kontur.vostok.hercules.sink.parallel;

import java.util.concurrent.TimeUnit;

/**
 * Thread synchronization functions
 *
 * @author Innokentiy Krivonosov
 */
public interface ConcurrentTopicPartitionQueues extends TopicPartitionQueues {
    void awaitNotFull(long timeout, TimeUnit unit) throws InterruptedException;

    void signalNotFull();

    void awaitNotEmpty(long timeout, TimeUnit unit) throws InterruptedException;

    void signalNotEmpty();
}
