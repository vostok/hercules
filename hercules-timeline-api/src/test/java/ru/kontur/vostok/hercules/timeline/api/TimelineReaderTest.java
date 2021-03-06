package ru.kontur.vostok.hercules.timeline.api;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.internal.matchers.Matches;
import ru.kontur.vostok.hercules.cassandra.util.CassandraConnector;
import ru.kontur.vostok.hercules.health.Meter;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.health.Timer;
import ru.kontur.vostok.hercules.meta.timeline.Timeline;
import ru.kontur.vostok.hercules.protocol.TimelineSliceState;
import ru.kontur.vostok.hercules.protocol.TimelineState;
import ru.kontur.vostok.hercules.protocol.util.EventUtil;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TimelineReaderTest {

    private static class StatementMatcher implements ArgumentMatcher<SimpleStatement> {

        private final ArgumentMatcher<Object> cqlMatcher;
        private final String regexp;

        public StatementMatcher(String regexp) {
            this.cqlMatcher = new Matches(regexp);
            this.regexp = regexp;
        }

        @Override
        public boolean matches(SimpleStatement argument) {
            return cqlMatcher.matches(argument.getQuery());
        }

        @Override
        public String toString() {
            return "Expression matches \"" + regexp + "\"";
        }
    }

    private static final String EVENT_ID_REGEXP = "0x[0-9a-zA-Z]{48}";

    private static final Timeline TIMELINE = new Timeline();
    static {
        TIMELINE.setName("test-timeline");
        TIMELINE.setSlices(1);
        TIMELINE.setTimetrapSize(1_000);
    }

    private TimelineReader timelineReader;
    private CqlSession session = mock(CqlSession.class);
    private ResultSet resultSet = mock(ResultSet.class);
    private MetricsCollector metricsCollector = mock(MetricsCollector.class);

    @Before
    public void setUp() {
        CassandraConnector connector = mock(CassandraConnector.class);
        when(connector.session()).thenReturn(session);

        mockIterable(resultSet);

        when(session.execute(any(SimpleStatement.class))).thenReturn(resultSet);

        when(metricsCollector.meter(anyString())).thenReturn(n -> {
        });
        when(metricsCollector.timer(anyString())).thenReturn((duration, unit) -> {
        });

        timelineReader = new TimelineReader(new Properties(), connector, metricsCollector);
    }

    @Test
    public void shouldRequestTwoSlices() {
        Timeline timeline = new Timeline();
        timeline.setName("test-timeline");
        timeline.setSlices(2);
        timeline.setTimetrapSize(1_000);

        timelineReader.readTimeline(
                timeline,
                new TimelineState(new TimelineSliceState[]{}),
                0,
                1,
                10,
                0,
                10_000_000
        );

        verify(session).execute(argThat(cql(".+ slice = 0 .+")));
        verify(session).execute(argThat(cql(".+ slice = 1 .+")));
    }

    @Test
    public void shouldIncludeMinimalEventIdInRequestIfNoPartitionReadStatePassed() {
        timelineReader.readTimeline(
                TIMELINE,
                new TimelineState(new TimelineSliceState[]{}),
                0,
                1,
                1,
                0,
                10_000_000
        );

        verify(session).execute(argThat(cql(
                ".+ event_id >= " + EVENT_ID_REGEXP + " AND event_id < " + EVENT_ID_REGEXP + " .+"
        )));
    }

    @Test
    public void shouldNotIncludeMinimalEventIdInRequestIfPartitionReadStatePassed() {
        timelineReader.readTimeline(
                TIMELINE,
                new TimelineState(new TimelineSliceState[]{
                        new TimelineSliceState(0, 0, EventUtil.eventIdAsBytes(122_192_928_000_000_000L, UUID.fromString("13814000-1dd2-11b2-8000-000000000000")))
                }),
                0,
                1,
                1,
                0,
                10_000_000
        );

        verify(session).execute(argThat(cql(
                ".+ event_id > " + EVENT_ID_REGEXP + " AND event_id < " + EVENT_ID_REGEXP + " .+"
        )));
    }

    @Test
    public void shouldReadBetweenFromTimestampAndToTimestamp() {
        long from = TimeUtil.millisToTicks(100);
        long to = TimeUtil.millisToTicks(900);
        timelineReader.readTimeline(
                TIMELINE,
                new TimelineState(new  TimelineSliceState[0]),
                0,
                1,
                1,
                from,
                to);

        verify(session).execute(argThat(cql(
                ".+ event_id >= " + EventUtil.minEventIdForTimestampAsHexString(from) + " AND " +
                        "event_id < " + EventUtil.minEventIdForTimestampAsHexString(to) + " .+")));
    }

    public static <T> void mockIterable(Iterable<T> iterable, T... values) {
        Iterator<T> mockIterator = mock(Iterator.class);
        when(iterable.iterator()).thenReturn(mockIterator);

        if (values.length == 0) {
            when(mockIterator.hasNext()).thenReturn(false);
            return;
        } else if (values.length == 1) {
            when(mockIterator.hasNext()).thenReturn(true, false);
            when(mockIterator.next()).thenReturn(values[0]);
        } else {
            // build boolean array for hasNext()
            Boolean[] hasNextResponses = new Boolean[values.length];
            for (int i = 0; i < hasNextResponses.length - 1; i++) {
                hasNextResponses[i] = true;
            }
            hasNextResponses[hasNextResponses.length - 1] = false;
            when(mockIterator.hasNext()).thenReturn(true, hasNextResponses);
            T[] valuesMinusTheFirst = Arrays.copyOfRange(values, 1, values.length);
            when(mockIterator.next()).thenReturn(values[0], valuesMinusTheFirst);
        }
    }

    public static StatementMatcher cql(String regexp) {
        return new StatementMatcher(regexp);
    }
}
