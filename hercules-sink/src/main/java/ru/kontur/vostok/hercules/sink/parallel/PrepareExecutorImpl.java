package ru.kontur.vostok.hercules.sink.parallel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.kafka.util.serialization.EventDeserializer;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.sink.ProcessorResult;
import ru.kontur.vostok.hercules.sink.filter.EventFilter;
import ru.kontur.vostok.hercules.sink.metrics.ParallelPrepareStat;
import ru.kontur.vostok.hercules.sink.metrics.SinkMetrics;
import ru.kontur.vostok.hercules.sink.parallel.sender.AbstractParallelSender;
import ru.kontur.vostok.hercules.sink.parallel.sender.PreparedData;
import ru.kontur.vostok.hercules.util.time.TimeSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Process deserializing, filtering and preparing events batch
 *
 * @author Innokentiy Krivonosov
 */
class PrepareExecutorImpl<T extends PreparedData> implements PrepareExecutor<T> {
    private final static Logger LOGGER = LoggerFactory.getLogger(PrepareExecutorImpl.class);
    private final ThreadLocal<ParallelPrepareStat> stats = ThreadLocal.withInitial(() -> new ParallelPrepareStat(TimeSource.SYSTEM));

    private final EventDeserializer eventDeserializer = EventDeserializer.parseAllTags();
    private final AbstractParallelSender<T> parallelSender;
    private final List<EventFilter> filters;
    private final SinkMetrics metrics;

    public PrepareExecutorImpl(
            AbstractParallelSender<T> parallelSender,
            List<EventFilter> filters,
            SinkMetrics metrics) {
        this.parallelSender = parallelSender;
        this.filters = filters;
        this.metrics = metrics;
    }

    /**
     * Process prepare EventsBatch.
     *
     * @param eventsBatch events to be processed
     */
    @Override
    public void processPrepare(EventsBatch<T> eventsBatch) {
        try {
            ParallelPrepareStat stat = stats.get();
            stat.reset();

            deserializeAndFilterEvents(eventsBatch, stat);

            prepare(eventsBatch, stat);

            metrics.update(stat);
        } catch (Exception ex) {
            LOGGER.error("Exception on process prepare", ex);
        }
    }

    private void prepare(EventsBatch<T> eventsBatch, ParallelPrepareStat stat) {
        stat.markProcessStart();
        List<Event> events = eventsBatch.getAllEvents();
        try {
            T preparedData = parallelSender.prepare(events);
            eventsBatch.setPreparedData(preparedData);
            stat.setTotalEvents(events.size());
        } catch (Exception ex) {
            LOGGER.error("Unspecified exception acquired while prepare events", ex);
            stat.incrementDroppedEvents(events.size());
            eventsBatch.setProcessorResult(ProcessorResult.fail());
        } finally {
            stat.markProcessEnd();
        }
    }

    private void deserializeAndFilterEvents(EventsBatch<T> eventsBatch, ParallelPrepareStat stat) {
        eventsBatch.rawEvents.forEach((topicPartition, rawEvents) -> {
            List<Event> events = deserializeEvents(rawEvents, stat);
            List<Event> filteredEvents = filterEvents(events, stat);
            eventsBatch.events.put(topicPartition, filteredEvents);
        });
    }

    private List<Event> deserializeEvents(List<byte[]> rawEvents, ParallelPrepareStat stat) {
        List<Event> events = new ArrayList<>(rawEvents.size());

        stat.markDeserializationStart();
        for (byte[] rawEvent : rawEvents) {
            Event event = eventDeserializer.deserialize("", rawEvent);
            if (event != null) {
                events.add(event);
            } else {
                // Received non-deserializable data, should be ignored
                stat.incrementDroppedEvents();
            }
        }
        stat.markDeserializationEnd();

        return events;
    }

    private List<Event> filterEvents(List<Event> events, ParallelPrepareStat stat) {
        List<Event> filteredEvents = new ArrayList<>(events.size());
        stat.markFiltrationStart();
        try {
            for (Event event : events) {
                if (filter(event)) {
                    filteredEvents.add(event);
                } else {
                    stat.incrementFilteredEvents();
                }
            }
        } catch (Exception ex) {
            LOGGER.error("Unspecified exception acquired while filtration", ex);
        } finally {
            stat.markFiltrationEnd();
        }
        return filteredEvents;
    }

    private boolean filter(Event event) {
        for (EventFilter filter : filters) {
            if (!filter.test(event)) {
                return false;
            }
        }
        return true;
    }
}
