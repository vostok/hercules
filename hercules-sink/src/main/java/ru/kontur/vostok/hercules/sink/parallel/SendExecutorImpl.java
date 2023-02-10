package ru.kontur.vostok.hercules.sink.parallel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.sink.ProcessorResult;
import ru.kontur.vostok.hercules.sink.metrics.ParallelSendStat;
import ru.kontur.vostok.hercules.sink.metrics.SinkMetrics;
import ru.kontur.vostok.hercules.sink.parallel.sender.AbstractParallelSender;
import ru.kontur.vostok.hercules.sink.parallel.sender.PreparedData;
import ru.kontur.vostok.hercules.util.time.TimeSource;

/**
 * Process sending events batch, send metrics.
 *
 * @author Innokentiy Krivonosov
 */
class SendExecutorImpl<T extends PreparedData> implements SendExecutor<T> {
    private final static Logger LOGGER = LoggerFactory.getLogger(SendExecutorImpl.class);
    private final ThreadLocal<ParallelSendStat> stats = ThreadLocal.withInitial(() -> new ParallelSendStat(TimeSource.SYSTEM));

    private final AbstractParallelSender<T> parallelSender;
    private final SinkMetrics metrics;

    SendExecutorImpl(AbstractParallelSender<T> parallelSender, SinkMetrics metrics) {
        this.parallelSender = parallelSender;
        this.metrics = metrics;
    }

    /**
     * Process send EventsBatch.
     *
     * @param eventsBatch eventBatch to be processed
     */
    @Override
    public void processSend(EventsBatch<T> eventsBatch) {
        ParallelSendStat stat = this.stats.get();
        try {
            stat.reset();
            stat.markProcessSendStart();
            ProcessorResult processorResult = parallelSender.processSend(eventsBatch.getPreparedData());
            eventsBatch.setProcessorResult(processorResult);
        } catch (Exception ex) {
            LOGGER.error("Unspecified exception acquired while send events", ex);
            eventsBatch.setProcessorResult(ProcessorResult.fail());
        } finally {
            stat.markProcessSendEnd();

            metrics.update(stat);
        }
    }
}
