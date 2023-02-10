package ru.kontur.vostok.hercules.sink.parallel;

import ru.kontur.vostok.hercules.sink.parallel.sender.PreparedData;

/**
 * Preparing a batch for sending.
 *
 * @author Innokentiy Krivonosov
 */
public interface PrepareExecutor<T extends PreparedData> {
    void processPrepare(EventsBatch<T> eventsBatch);
}
