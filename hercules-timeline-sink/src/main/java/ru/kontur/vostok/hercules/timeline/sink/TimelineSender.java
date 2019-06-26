package ru.kontur.vostok.hercules.timeline.sink;

import ru.kontur.vostok.hercules.cassandra.sink.CassandraSender;
import ru.kontur.vostok.hercules.cassandra.util.Slicer;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.kafka.util.processing.BackendServiceFailedException;
import ru.kontur.vostok.hercules.meta.filter.Filter;
import ru.kontur.vostok.hercules.meta.timeline.TimeTrapUtil;
import ru.kontur.vostok.hercules.meta.timeline.Timeline;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.util.EventUtil;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 *
 * @author Gregory Koshelev
 */
public class TimelineSender extends CassandraSender {
    private final Timeline timeline;
    private final Slicer slicer;
    private final Predicate<Event> predicate;

    public TimelineSender(Timeline timeline, Slicer slicer, Properties properties, MetricsCollector metricsCollector) {
        super(properties, metricsCollector);

        this.timeline = timeline;
        this.slicer = slicer;

        final Filter[] filters = timeline.getFilters();
        this.predicate = event -> {
            for (Filter filter : filters) {
                if (!filter.test(event.getPayload())) {
                    return false;
                }
            }
            return true;
        };
    }

    @Override
    protected int send(List<Event> events) throws BackendServiceFailedException {
        return super.send(filter(events));
    }

    @Override
    protected String query() {
        return "INSERT INTO " + TimelineUtil.timelineToTableName(timeline) + " (" +
                " slice," +
                " tt_offset," +
                " event_id," +
                " payload" +
                ") " +
                "VALUES (?, ?, ?, ?)";
    }

    @Override
    protected Optional<Object[]> convert(Event event) {
        int slice = slicer.slice(event);
        long ttOffset = TimeTrapUtil.toTimeTrapOffset(timeline.getTimetrapSize(), event.getTimestamp());
        ByteBuffer eventId = EventUtil.eventIdAsByteBuffer(event.getTimestamp(), event.getUuid());
        ByteBuffer payload = ByteBuffer.wrap(event.getBytes());

        return Optional.of(new Object[] {
                slice,
                ttOffset,
                eventId,
                payload
        });
    }

    private List<Event> filter(List<Event> events) {
        if (timeline.getFilters().length == 0) {
            return events;
        }

        return events.stream().filter(predicate).collect(Collectors.toList());
    }
}
