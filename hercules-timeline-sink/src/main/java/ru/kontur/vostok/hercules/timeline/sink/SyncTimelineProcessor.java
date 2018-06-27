package ru.kontur.vostok.hercules.timeline.sink;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import org.apache.kafka.streams.processor.AbstractProcessor;
import ru.kontur.vostok.hercules.cassandra.util.CassandraConnector;
import ru.kontur.vostok.hercules.cassandra.util.Slicer;
import ru.kontur.vostok.hercules.meta.timeline.Timeline;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * @author Gregory Koshelev
 */
public class SyncTimelineProcessor extends AbstractProcessor<UUID, Event> {
    private final Session session;
    private final PreparedStatement prepared;
    private final Timeline timeline;
    private final Slicer slicer;

    public SyncTimelineProcessor(CassandraConnector connector, Timeline timeline, Slicer slicer) {
        session = connector.session();

        PreparedStatement prepared =
                session.prepare("INSERT INTO " + TimelineUtil.timelineToTableName(timeline) + " (slice_id, tt_offset, event_timestamp, event_id, payload) VALUES (?, ?, ?, ?, ?)");
        prepared.setConsistencyLevel(ConsistencyLevel.QUORUM);
        this.prepared = prepared;

        this.timeline = timeline;
        this.slicer = slicer;
    }

    @Override
    public void process(UUID key, Event value) {
        int slice = slicer.slice(value);
        long timestamp = TimeUtil.gregorianTicksToUnixTime(value.getId().timestamp());
        long ttOffset = TimeTrapUtil.toTimeTrapOffset(timeline.getTimetrapSize(), timestamp);
        byte[] payload = value.getBytes();
        BoundStatement statement = prepared.bind(slice, ttOffset, timestamp, key, ByteBuffer.wrap(payload));
        try {
            ResultSet result = session.execute(statement);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
