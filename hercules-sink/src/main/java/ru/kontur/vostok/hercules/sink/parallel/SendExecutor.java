package ru.kontur.vostok.hercules.sink.parallel;

import ru.kontur.vostok.hercules.sink.parallel.sender.PreparedData;

/**
 * Process sending events batch.
 *
 * @author Innokentiy Krivonosov
 */
public interface SendExecutor<T extends PreparedData> {
    void processSend(EventsBatch<T> eventsBatch);
}
