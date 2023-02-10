package ru.kontur.vostok.hercules.sink.parallel;

import ru.kontur.vostok.hercules.sink.parallel.sender.PreparedData;

/**
 * Interface of the classes that will listen process batch events.
 * <p>
 * The key difference is that detailed information about events {@link EventsBatch} will be available.
 *
 * @author Innokentiy Krivonosov
 */
public interface EventsBatchListener<T extends PreparedData> {

    /**
     * Execute after finish prepare
     *
     * @param batch events batch
     */
    void onFinishPrepare(EventsBatch<T> batch);
}
