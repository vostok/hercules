package ru.kontur.vostok.hercules.timeline.sink;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import org.apache.kafka.streams.processor.AbstractProcessor;
import ru.kontur.vostok.hercules.cassandra.util.CassandraConnector;
import ru.kontur.vostok.hercules.cassandra.util.Slicer;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.meta.timeline.TimeTrapUtil;
import ru.kontur.vostok.hercules.meta.timeline.Timeline;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.util.EventUtil;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class SyncTimelineProcessor extends AbstractProcessor<UUID, Event> {
    private final CqlSession session;
    private final PreparedStatement prepared;
    private final Timeline timeline;
    private final Slicer slicer;
    private final Meter processedEventCountMeter;
    private final Timer eventProcessTimer;

    public SyncTimelineProcessor(
            CassandraConnector connector,
            Timeline timeline,
            Slicer slicer,
            MetricsCollector metricsCollector
    ) {
        session = connector.session();

        this.prepared = session.prepare(
                "INSERT INTO " + TimelineUtil.timelineToTableName(timeline) +
                        " (slice, tt_offset, event_id, payload)" +
                        " VALUES (?, ?, ?, ?)");

        this.timeline = timeline;
        this.slicer = slicer;

        processedEventCountMeter = metricsCollector.meter("processedEventCount");
        eventProcessTimer = metricsCollector.timer("processDurationMs");
    }

    @Override
    public void process(UUID key, Event value) {
        long startOfProcess = System.currentTimeMillis();
        int slice = slicer.slice(value);
        long ttOffset = TimeTrapUtil.toTimeTrapOffset(timeline.getTimetrapSize(), value.getTimestamp());
        ByteBuffer eventId = EventUtil.eventIdAsByteBuffer(value.getTimestamp(), value.getUuid());
        byte[] payload = value.getBytes();
        BoundStatement statement = prepared.bind(slice, ttOffset, eventId, ByteBuffer.wrap(payload));
        try {
            ResultSet result = session.execute(statement);
            processedEventCountMeter.mark();
            eventProcessTimer.update(System.currentTimeMillis() - startOfProcess, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
