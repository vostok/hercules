package ru.kontur.vostok.hercules.sink.parallel.sender;

import ru.kontur.vostok.hercules.health.IMetricsCollector;
import ru.kontur.vostok.hercules.kafka.util.processing.BackendServiceFailedException;
import ru.kontur.vostok.hercules.protocol.Event;

import java.util.List;
import java.util.Properties;

/**
 * Sender with a skipped prepare step
 *
 * @author Innokentiy Krivonosov
 */
public abstract class NoPrepareParallelSender extends AbstractParallelSender<NoPrepareParallelSender.NoPrepareEvents> {

    public NoPrepareParallelSender(Properties properties, IMetricsCollector metricsCollector) {
        super(properties, metricsCollector);
    }

    @Override
    public final NoPrepareEvents prepare(List<Event> events) {
        return new NoPrepareEvents(events);
    }

    @Override
    public final int send(NoPrepareEvents preparedData) throws BackendServiceFailedException {
        return send(preparedData.events);
    }

    /**
     * Minimal implementation of {@link PreparedData} used to send events without prepare.
     */
    public static class NoPrepareEvents implements PreparedData {
        final List<Event> events;

        NoPrepareEvents(List<Event> events) {
            this.events = events;
        }

        @Override
        public int getEventsCount() {
            return events.size();
        }
    }
}
