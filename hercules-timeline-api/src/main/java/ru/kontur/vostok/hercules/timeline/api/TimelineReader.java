package ru.kontur.vostok.hercules.timeline.api;

import com.datastax.driver.core.*;
import ru.kontur.vostok.hercules.meta.timeline.Timeline;
import ru.kontur.vostok.hercules.meta.timeline.TimelineUtil;
import ru.kontur.vostok.hercules.partitioner.LogicalPartitioner;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventId;
import ru.kontur.vostok.hercules.protocol.TimelineReadState;
import ru.kontur.vostok.hercules.protocol.TimelineShardReadState;
import ru.kontur.vostok.hercules.protocol.decoder.Decoder;
import ru.kontur.vostok.hercules.protocol.decoder.EventReader2;

import java.lang.reflect.Array;
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

    public static final String SELECT_EVENTS =
            "select" +
            "  slice," +
            "  tt_offset," +
            "  event_timestamp," +
            "  event_id," +
            "  payload" +
            " " +
            "from" +
            "  timelines.%s" + // table name
            " " +
            "where" +
            "  slice=%d and" + // partition
            "  tt_offset in (%s) and" + // tt_offsets array
            "  event_timestamp >= %d and" + // from
            "  event_timestamp < %d and" + // to
            "  event_id > %s" + // last red event id
            " " +
            "order by " +
            "  event_timestamp," +
            "  event_id" +
            " " +
            "limit %d " +  // take count
            "allow filtering";

    private static final EventReader2 EVENT_READER = EventReader2.readNoTags();

    private final Cluster cluster;

    public TimelineReader() {
        cluster = Cluster.builder()
                .addContactPoint("localhost")
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

        String ttOffsets = Arrays.stream(getTimetrapOffsets(from, toInclusive, timeline.getTimetrapSize()))
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(","));

        List<Event> result = new LinkedList<>();

        Session session = cluster.connect();

        for (int partition : partitions) {
            TimelineShardReadStateOffset offset = offsetMap.computeIfAbsent(partition, i -> new TimelineShardReadStateOffset(0, getMinimalUuid()));

            SimpleStatement statement = new SimpleStatement(String.format(
                    SELECT_EVENTS,
                    timeline.getName(),
                    partition,
                    ttOffsets,
                    offset.eventTimestamp,
                    to,
                    offset.eventId.toString(),
                    take
            ));
            statement.setFetchSize(Integer.MAX_VALUE);

            System.out.println(statement.toString());

            ResultSet rows = session.execute(statement);

            take -= rows.getAvailableWithoutFetching();

            rows.forEach(row -> {
                offset.eventTimestamp = row.getLong("event_timestamp");
                offset.eventId = row.getUUID("event_id");
                result.add(EVENT_READER.read(new Decoder(row.getBytes("payload").array())));
            });

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

    private static UUID getMinimalUuid() {
        return UUID.fromString("00000000-0000-1000-0000-000000000000");
    }
}
