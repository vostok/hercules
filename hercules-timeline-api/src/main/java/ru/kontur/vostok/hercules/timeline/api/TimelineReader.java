package ru.kontur.vostok.hercules.timeline.api;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.cassandra.util.CassandraConnector;
import ru.kontur.vostok.hercules.health.Meter;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.health.Timer;
import ru.kontur.vostok.hercules.meta.timeline.TimeTrapUtil;
import ru.kontur.vostok.hercules.meta.timeline.Timeline;
import ru.kontur.vostok.hercules.partitioner.LogicalPartitioner;
import ru.kontur.vostok.hercules.protocol.TimelineByteContent;
import ru.kontur.vostok.hercules.protocol.TimelineSliceState;
import ru.kontur.vostok.hercules.protocol.TimelineState;
import ru.kontur.vostok.hercules.protocol.util.EventUtil;
import ru.kontur.vostok.hercules.util.bytes.ByteUtil;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Read event timeline from Cassandra cluster
 * <p>
 * FIXME: Should be revised and refactored
 */
public class TimelineReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimelineReader.class);

    /**
     * Utility class to store shard read offset
     */
    private static class TimelineShardReadStateOffset {
        long ttOffset;
        byte[] eventId;

        public TimelineShardReadStateOffset(long ttOffset, byte[] eventId) {
            this.ttOffset = ttOffset;
            this.eventId = eventId;
        }
    }

    /**
     * Iterable values for GridIterator
     */
    private static class Parameters {
        final int slice;
        final long ttOffset;

        public Parameters(int slice, long ttOffset) {
            this.slice = slice;
            this.ttOffset = ttOffset;
        }
    }

    /**
     * GridIterator iterate over slice and tt_offset parameters grid
     */
    private static class GridIterator implements Iterator<Parameters> {
        int[] slices;
        long[] ttOffsets;

        int sliceCurrentIdx = 0;
        int ttOffsetCurrentIdx = 0;

        public GridIterator(int[] slices, long[] ttOffsets) {
            this.slices = slices;
            this.ttOffsets = ttOffsets;
        }

        @Override
        public boolean hasNext() {
            return ttOffsetCurrentIdx < ttOffsets.length;
        }

        @Override
        public Parameters next() {
            if (ttOffsets.length <= ttOffsetCurrentIdx) {
                throw new NoSuchElementException();
            }
            Parameters result = new Parameters(slices[sliceCurrentIdx], ttOffsets[ttOffsetCurrentIdx]);
            ++sliceCurrentIdx;
            if (sliceCurrentIdx == slices.length) {
                ++ttOffsetCurrentIdx;
                sliceCurrentIdx = 0;
            }
            return result;
        }
    }

    /**
     * Iterable to create GridIterator
     */
    private static class Grid implements Iterable<Parameters> {
        int[] slices;
        long[] ttOffsets;

        public Grid(int[] slices, long[] ttOffsets) {
            this.slices = slices;
            this.ttOffsets = ttOffsets;
        }

        @Override
        public Iterator<Parameters> iterator() {
            return new GridIterator(slices, ttOffsets);
        }
    }

    private static final byte[] NIL = new byte[24];

    private static boolean isNil(byte[] eventId) {
        return Arrays.equals(NIL, eventId);
    }

    private static final String EVENT_ID = "event_id";
    private static final String PAYLOAD = "payload";

    private static final String SELECT_EVENTS = "" +
            "SELECT" +
            " event_id," +
            " payload" +
            " " +
            "FROM" +
            " %s" +
            " " +
            "WHERE" +
            " slice = %d AND" +
            " tt_offset = %d AND" +
            " event_id > %s AND" + // Lower bound
            " event_id < %s" + // Upper bound
            " " +
            "ORDER BY" +
            " event_id" +
            " " +
            "LIMIT %d;";

    private static final String SELECT_EVENTS_START_READING_SLICE = "" +
            "SELECT" +
            " event_id," +
            " payload" +
            " " +
            "FROM" +
            " %s" +
            " " +
            "WHERE" +
            " slice = %d AND" +
            " tt_offset = %d AND" +
            " event_id >= %s AND" + // Lower bound
            " event_id < %s" + // Upper bound
            " " +
            "ORDER BY" +
            " event_id" +
            " " +
            "LIMIT %d;";

    private final CqlSession session;
    private final int timetrapCountLimit;

    private final MetricsCollector metricsCollector;
    private final Meter receivedEventsCountMeter;
    private final Meter receivedBytesCountMeter;
    private final Timer readingTimer;

    public TimelineReader(
            Properties properties,
            CassandraConnector connector,
            MetricsCollector metricsCollector) {
        this.session = connector.session();
        this.timetrapCountLimit = PropertiesUtil.get(Props.TIMETRAP_COUNT_LIMIT, properties).get();
        this.metricsCollector = metricsCollector;
        this.receivedEventsCountMeter = metricsCollector.meter("receivedEventsCount");
        this.receivedBytesCountMeter = metricsCollector.meter("receivedBytesCount");
        this.readingTimer = metricsCollector.timer("readingTimeMs");
    }

    /**
     * Read timeline content from Cassandra cluster
     *
     * @param timeline   timeline info
     * @param readState  offsets data
     * @param shardIndex parameter for logical partitioning
     * @param shardCount parameter for logical partitioning
     * @param take       fetch size
     * @param from       lower timestamp bound in 100-ns ticks from Unix epoch
     * @param to         upper timestamp bound exclusive in 100-ns ticks from Unix epoch
     * @return timeline content
     */
    public TimelineByteContent readTimeline(
            Timeline timeline,
            TimelineState readState,
            int shardIndex,
            int shardCount,
            int take,
            long from,
            long to
    ) {
        TimelineMetricsCollector timelineMetricsCollector = new TimelineMetricsCollector(metricsCollector, timeline.getName());
        final long start = System.currentTimeMillis();

        long toInclusive = to - 1;
        long timetrapSize = timeline.getTimetrapSize();

        int[] partitions = LogicalPartitioner.getPartitionsForLogicalSharding(timeline, shardIndex, shardCount);
        if (partitions.length == 0) {
            return new TimelineByteContent(readState, new byte[][]{});
        }
        long[] timetrapOffsets = TimeTrapUtil.getTimetrapOffsets(from, toInclusive, timetrapSize);

        Map<Integer, TimelineShardReadStateOffset> offsetMap = toMap(readState);

        List<byte[]> result = new LinkedList<>();

        for (Parameters params : new Grid(partitions, timetrapOffsets)) {
            TimelineShardReadStateOffset offset = offsetMap.computeIfAbsent(
                    params.slice,
                    i -> getEmptyReadStateOffset(params.ttOffset)
            );
            if (params.ttOffset < offset.ttOffset) {
                continue; // Skip already red timetrap offsets
            } else if (offset.ttOffset < params.ttOffset) {
                offsetMap.put(params.slice, getEmptyReadStateOffset(params.ttOffset));
                offset = offsetMap.get(params.slice);
            }

            SimpleStatement statement = generateStatement(timeline, params, offset, take, from, to);

            LOGGER.info("Executing '{}'", statement.toString());

            ResultSet rows = session.execute(statement);
            for (Row row : rows) {
                offset.eventId = ByteUtil.fromByteBuffer(row.getByteBuffer(EVENT_ID));
                result.add(row.getByteBuffer(PAYLOAD).array());
                --take;
            }
            // If no rows were fetched increment tt_offset to mark partition (slice, offset_id) as red
            if (isNil(offset.eventId)) {
                offset.ttOffset += timeline.getTimetrapSize();
            }

            if (take <= 0) {
                break;
            }
        }

        int sizeOfEvents = 0;
        for (byte[] event : result) {
            sizeOfEvents += event.length;
        }
        receivedBytesCountMeter.mark(sizeOfEvents);
        timelineMetricsCollector.markReceivedBytesCount(sizeOfEvents);
        receivedEventsCountMeter.mark(result.size());
        timelineMetricsCollector.markReceivedEventsCount(result.size());
        final long readingDurationMs = System.currentTimeMillis() - start;
        readingTimer.update(readingDurationMs, TimeUnit.MILLISECONDS);
        timelineMetricsCollector.updateReadingTime(readingDurationMs);

        return new TimelineByteContent(toState(offsetMap), result.toArray(new byte[0][]));
    }

    public int getTimetrapCountLimit() {
        return timetrapCountLimit;
    }

    public void shutdown() {
        session.close();
    }

    private static TimelineShardReadStateOffset getEmptyReadStateOffset(long ttOffset) {
        return new TimelineShardReadStateOffset(ttOffset, NIL);
    }

    private static SimpleStatement generateStatement(Timeline timeline, Parameters params, TimelineShardReadStateOffset offset, int take, long from, long to) {
        if (!isNil(offset.eventId)) {
            return SimpleStatement.builder(String.format(
                    SELECT_EVENTS,
                    timeline.getName(),
                    params.slice,
                    params.ttOffset,
                    EventUtil.eventIdOfBytesAsHexString(offset.eventId),
                    EventUtil.minEventIdForTimestampAsHexString(Math.min(to, TimeUtil.millisToTicks(params.ttOffset + timeline.getTimetrapSize()))),
                    take
            )).setPageSize(take).build();
        } else {
            return SimpleStatement.builder(String.format(
                    SELECT_EVENTS_START_READING_SLICE,
                    timeline.getName(),
                    params.slice,
                    params.ttOffset,
                    EventUtil.minEventIdForTimestampAsHexString(Math.max(from, TimeUtil.millisToTicks(params.ttOffset))),
                    EventUtil.minEventIdForTimestampAsHexString(Math.min(to, TimeUtil.millisToTicks(params.ttOffset + timeline.getTimetrapSize()))),
                    take
            )).setPageSize(take).build();
        }
    }

    private static Map<Integer, TimelineShardReadStateOffset> toMap(TimelineState readState) {
        return Arrays.stream(readState.getSliceStates())
                .collect(Collectors.toMap(
                        TimelineSliceState::getSlice,
                        shardState -> new TimelineShardReadStateOffset(
                                shardState.getTtOffset(),
                                shardState.getEventId()
                        )
                ));
    }

    private static TimelineState toState(Map<Integer, TimelineShardReadStateOffset> offsetMap) {
        return new TimelineState(offsetMap.entrySet().stream()
                .map(offsetEntry -> new TimelineSliceState(
                        offsetEntry.getKey(),
                        offsetEntry.getValue().ttOffset,
                        offsetEntry.getValue().eventId
                ))
                .toArray(TimelineSliceState[]::new)
        );
    }

    static class Props {
        static final Parameter<Integer> TIMETRAP_COUNT_LIMIT =
                Parameter.integerParameter("timetrapCountLimit").
                        withDefault(30).
                        build();
    }
}
