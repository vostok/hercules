package ru.kontur.vostok.hercules.timeline.api;

import com.datastax.driver.core.*;
import ru.kontur.vostok.hercules.meta.timeline.Timeline;
import ru.kontur.vostok.hercules.meta.timeline.TimelineUtil;
import ru.kontur.vostok.hercules.partitioner.LogicalPartitioner;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventId;
import ru.kontur.vostok.hercules.protocol.TimelineContent;
import ru.kontur.vostok.hercules.protocol.TimelineReadState;
import ru.kontur.vostok.hercules.protocol.TimelineShardReadState;
import ru.kontur.vostok.hercules.protocol.decoder.Decoder;
import ru.kontur.vostok.hercules.protocol.decoder.EventReader2;

import java.util.*;
import java.util.stream.Collectors;


public class TimelineReader {

    private static class TimelineShardReadStateOffset {
        long eventTimestamp;
        UUID eventId;

        public TimelineShardReadStateOffset(long eventTimestamp, UUID eventId) {
            this.eventTimestamp = eventTimestamp;
            this.eventId = eventId;
        }
    }

    private static class Parameters {
        final int slice;
        final long ttOffset;

        public Parameters(int slice, long ttOffset) {
            this.slice = slice;
            this.ttOffset = ttOffset;
        }
    }

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

    public static final String SELECT_EVENTS_BY_TIMESTAMP = "" +
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

    public static final String SELECT_EVENTS_BY_EVENT_ID = "" +
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

    private static final EventReader2 EVENT_READER = EventReader2.readNoTags();

    private final Cluster cluster;

    public TimelineReader(Properties properties) {
        cluster = Cluster.builder()
                .addContactPoint(properties.getProperty("contact.point"))
                .withoutMetrics() // To avoid java.lang.ClassNotFoundException: com.codahale.metrics.JmxReporter
                .build();
    }

    public TimelineContent readTimeline(
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
            return new TimelineContent(readState, new Event[0]);
        }
        long[] timetrapOffsets = getTimetrapOffsets(from, toInclusive, timeline.getTimetrapSize());

        List<Event> result = new LinkedList<>();
        Session session = cluster.connect();
        for (Parameters params : new Grid(partitions, timetrapOffsets)) {
            TimelineShardReadStateOffset offset = offsetMap.computeIfAbsent(params.slice, i -> new TimelineShardReadStateOffset(0, null));

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
            statement.setFetchSize(Integer.MAX_VALUE);

            System.out.println("Executing '" + statement.toString() + "'");

            ResultSet rows = session.execute(statement);
            for (Row row : rows) {
                offset.eventTimestamp = row.getLong("event_timestamp");
                offset.eventId = row.getUUID("event_id");
                result.add(EVENT_READER.read(new Decoder(row.getBytes("payload").array())));
                --take;
            }

            if (take <= 0) {
                break;
            }
        }
        return new TimelineContent(toState(offsetMap), result.toArray(new Event[0]));
    }

    public void shutdown() {
        cluster.close();
    }

    private static Map<Integer, TimelineShardReadStateOffset> toMap(TimelineReadState readState) {
        return Arrays.stream(readState.getShards()).collect(Collectors.toMap(
                TimelineShardReadState::getShardId,
                shardState -> new TimelineShardReadStateOffset(
                        shardState.getEventTimestamp(),
                        new UUID(shardState.getEventId().getP1(), shardState.getEventId().getP2())
                )
        ));
    }

    private static TimelineReadState toState(Map<Integer, TimelineShardReadStateOffset> offsetMap) {
        return new TimelineReadState(offsetMap.entrySet().stream()
                .map(offsetEntry -> new TimelineShardReadState(
                        offsetEntry.getKey(),
                        offsetEntry.getValue().eventTimestamp,
                        new EventId(
                                offsetEntry.getValue().eventId.getMostSignificantBits(),
                                offsetEntry.getValue().eventId.getLeastSignificantBits()
                        )
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
