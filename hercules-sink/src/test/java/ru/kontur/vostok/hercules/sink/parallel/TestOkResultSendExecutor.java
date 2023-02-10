package ru.kontur.vostok.hercules.sink.parallel;

import ru.kontur.vostok.hercules.sink.ProcessorResult;

/**
 * @author Innokentiy Krivonosov
 */
public class TestOkResultSendExecutor implements SendExecutor<TestPreparedData> {
    @Override
    public void processSend(EventsBatch<TestPreparedData> eventsBatch) {
        ProcessorResult okResult = ProcessorResult.ok(eventsBatch.getPreparedData().getEventsCount(), 0);
        eventsBatch.setProcessorResult(okResult);
    }
}
