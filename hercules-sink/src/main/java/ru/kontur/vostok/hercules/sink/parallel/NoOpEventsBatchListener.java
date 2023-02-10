package ru.kontur.vostok.hercules.sink.parallel;

import ru.kontur.vostok.hercules.sink.parallel.sender.PreparedData;

/**
 * Default implementation without any operations.
 *
 * @author Innokentiy Krivonosov
 */
public class NoOpEventsBatchListener<T extends PreparedData> implements EventsBatchListener<T> {
    @Override
    public void onFinishPrepare(EventsBatch<T> batch) {

    }
}
