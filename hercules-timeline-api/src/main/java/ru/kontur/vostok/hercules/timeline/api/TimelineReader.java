package ru.kontur.vostok.hercules.timeline.api;

import com.datastax.driver.core.*;
import ru.kontur.vostok.hercules.meta.timeline.Timeline;
import ru.kontur.vostok.hercules.meta.timeline.TimelineUtil;
import ru.kontur.vostok.hercules.partitioner.LogicalPartitioner;
import ru.kontur.vostok.hercules.protocol.TimelineByteContent;
import ru.kontur.vostok.hercules.protocol.TimelineReadState;
import ru.kontur.vostok.hercules.protocol.TimelineShardReadState;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Read event timeline from Cassandra cluster
 */
public class TimelineReader {

    /**
     * Utility class to store shard read offset
     */
    private static class TimelineShardReadStateOffset {
        long eventTimestamp;
        UUID eventId;

        public TimelineShardReadStateOffset(long eventTimestamp, UUID eventId) {
            this.eventTimestamp = eventTimestamp;
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

    private static final String EVENT_TIMESTAMP = "event_timestamp";
    private static final String EVENT_ID = "event_id";
    private static final String PAYLOAD = "payload";

    private static final String SELECT_EVENTS_BY_TIMESTAMP = "" +
            "SELECT" +
            "  event_timestamp," +
            "  event_id," +
            "  payload" +
            " " +
            "FROM" +
            "  timelines.%s" +
            " " +
            "WHERE" +
            "  slice = %d AND" +
            "  tt_offset = %d AND" +
            "  event_timestamp >= %d AND" +
            "  event_timestamp < %d" +
            " " +
            "ORDER BY " +
            "  event_timestamp," +
            "  event_id" +
            " " +
            "LIMIT %d;";

    private static final String SELECT_EVENTS_BY_EVENT_ID = "" +
            "SELECT" +
            "  event_timestamp," +
            "  event_id," +
            "  payload" +
            " " +
            "FROM" +
            "  timelines.%s" +
            " " +
            "WHERE" +
            "  slice = %d AND" +
            "  tt_offset = %d AND" +
            "  (event_timestamp, event_id) > (%d, %s) AND" +
            "  event_timestamp < %d" +
            " " +
            "ORDER BY " +
            "  event_timestamp," +
            "  event_id" +
            " " +
            "LIMIT %d;";

    private static final String CONTACT_POINT_PROPERTY = "contact.point";

    private final Cluster cluster;

    public TimelineReader(Properties properties) {
        cluster = Cluster.builder()
                .addContactPoint(properties.getProperty(CONTACT_POINT_PROPERTY))
                .withoutMetrics() // To avoid java.lang.ClassNotFoundException: com.codahale.metrics.JmxReporter
                .build();
    }

    /**
     * Read timeline content from Cassandra cluster
     * @param timeline timeline info
     * @param readState offsets data
     * @param k parameter for logical partitioning
     * @param n parameter for logical partitioning
     * @param take fetch size
     * @param from lower timestamp bound
     * @param to upper timestamp bound exclusive
     * @return timeline content
     */
    public TimelineByteContent readTimeline(
            Timeline timeline,
            TimelineReadState readState,
            int k,
            int n,
            int take,
            long from,
            long to
    ) {
        long toInclusive = to - 1;

        Map<Integer, TimelineShardReadStateOffset> offsetMap = toMap(readState);

        int[] partitions = LogicalPartitioner.getPartitionsForLogicalSharding(timeline, k, n);
        if (partitions.length == 0) {
            return new TimelineByteContent(readState, new byte[][]{});
        }
        long[] timetrapOffsets = getTimetrapOffsets(from, toInclusive, timeline.getTimetrapSize());

        List<byte[]> result = new LinkedList<>();
        try (Session session = cluster.connect()) {
            for (Parameters params : new Grid(partitions, timetrapOffsets)) {
                TimelineShardReadStateOffset offset = offsetMap.computeIfAbsent(params.slice, i -> getEmptyReadStateOffset());

                SimpleStatement statement = generateStatement(timeline, params, offset, from, to, take);
                statement.setFetchSize(Integer.MAX_VALUE); // fetch size defined by 'take' parameter

                // TODO: Change to logging
                System.out.println("Executing '" + statement.toString() + "'");

                ResultSet rows = session.execute(statement);
                for (Row row : rows) {
                    offset.eventTimestamp = row.getLong(EVENT_TIMESTAMP);
                    offset.eventId = row.getUUID(EVENT_ID);
                    result.add(row.getBytes(PAYLOAD).array());
                    --take;
                }

                if (take <= 0) {
                    break;
                }
            }
        }
        return new TimelineByteContent(toState(offsetMap), result.toArray(new byte[0][]));
    }

    public void shutdown() {
        cluster.close();
    }

    private TimelineShardReadStateOffset getEmptyReadStateOffset() {
        return new TimelineShardReadStateOffset(0, null);
    }

    private static SimpleStatement generateStatement(Timeline timeline, Parameters params, TimelineShardReadStateOffset offset, long from, long to, int take) {
        SimpleStatement statement;
        if (Objects.isNull(offset.eventId)) {
            statement = new SimpleStatement(String.format(
                    SELECT_EVENTS_BY_TIMESTAMP,
                    timeline.getName(),
                    params.slice,
                    params.ttOffset,
                    from,
                    to,
                    take
            ));
        }
        else {
            statement = new SimpleStatement(String.format(
                    SELECT_EVENTS_BY_EVENT_ID,
                    timeline.getName(),
                    params.slice,
                    params.ttOffset,
                    offset.eventTimestamp,
                    offset.eventId,
                    to,
                    take
            ));
        }
        return statement;
    }

    private static Map<Integer, TimelineShardReadStateOffset> toMap(TimelineReadState readState) {
        return Arrays.stream(readState.getShards())
                .filter(shardState -> Objects.nonNull(shardState.getEventId())) // Shard states without event id wasn't processed
                .collect(Collectors.toMap(
                        TimelineShardReadState::getShardId,
                        shardState -> new TimelineShardReadStateOffset(
                                shardState.getEventTimestamp(),
                                shardState.getEventId()
                        )
                ));
    }

    private static TimelineReadState toState(Map<Integer, TimelineShardReadStateOffset> offsetMap) {
        return new TimelineReadState(offsetMap.entrySet().stream()
                .map(offsetEntry -> new TimelineShardReadState(
                        offsetEntry.getKey(),
                        offsetEntry.getValue().eventTimestamp,
                        offsetEntry.getValue().eventId
                ))
                .toArray(TimelineShardReadState[]::new)
        );
    }

    private static long[] getTimetrapOffsets(long from, long to, long timetrapSize) {
        long fromTimetrap = TimelineUtil.calculateTimetrapOffset(from, timetrapSize);
        long toTimetrapExclusive = TimelineUtil.calculateNextTimetrapOffset(to, timetrapSize);

        int size = (int)((toTimetrapExclusive - fromTimetrap) / timetrapSize);

        long[] result = new long[size];
        long currentTimetrap = fromTimetrap;
        for (int i = 0; i < size; ++i) {
            result[i] = currentTimetrap;
            currentTimetrap += timetrapSize;
        }
        return result;
    }
}
