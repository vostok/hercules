package ru.kontur.vostok.hercules.sink.parallel.sender;

/**
 * Interface of data created in prepare stage
 *
 * @author Innokentiy Krivonosov
 */
public interface PreparedData {

    /**
     * Count of events to send.
     * Used to create a {@link ru.kontur.vostok.hercules.sink.ProcessorResult#ok(int, int)}} in {@link AbstractParallelSender}
     *
     * @return count of events to send
     */
    int getEventsCount();
}
