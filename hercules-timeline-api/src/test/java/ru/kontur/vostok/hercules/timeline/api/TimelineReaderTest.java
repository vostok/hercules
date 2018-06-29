package ru.kontur.vostok.hercules.timeline.api;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleStatement;
import com.datastax.driver.core.Statement;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import ru.kontur.vostok.hercules.cassandra.util.CassandraConnector;
import ru.kontur.vostok.hercules.meta.timeline.Timeline;
import ru.kontur.vostok.hercules.protocol.TimelineByteContent;
import ru.kontur.vostok.hercules.protocol.TimelineReadState;
import ru.kontur.vostok.hercules.protocol.TimelineShardReadState;

import java.util.Arrays;
import java.util.Iterator;

import static org.junit.Assert.*;

public class TimelineReaderTest {

    private static final Timeline TIMELINE = new Timeline();
    static {
        TIMELINE.setName("test-timeline");
        TIMELINE.setSlices(2);
        TIMELINE.setTimetrapSize(10_000);
    }

    private TimelineReader timelineReader;
    private Session session = Mockito.mock(Session.class);
    private ResultSet resultSet = Mockito.mock(ResultSet.class);

    @Before
    public void setUp() throws Exception {
        CassandraConnector connector = Mockito.mock(CassandraConnector.class);
        Mockito.when(connector.session()).thenReturn(session);

        mockIterable(resultSet);

        Mockito.when(session.execute(Mockito.any(SimpleStatement.class))).thenReturn(resultSet);

        timelineReader = new TimelineReader(connector);
    }

    @Test
    public void shouldRequestSomething() {

        TimelineByteContent content = timelineReader.readTimeline(
                TIMELINE,
                new TimelineReadState(new TimelineShardReadState[]{}),
                0,
                1,
                10,
                0,
                100_000
        );


    }

    public static <T> void mockIterable(Iterable<T> iterable, T... values) {
        Iterator<T> mockIterator = Mockito.mock(Iterator.class);
        Mockito.when(iterable.iterator()).thenReturn(mockIterator);

        if (values.length == 0) {
            Mockito.when(mockIterator.hasNext()).thenReturn(false);
            return;
        } else if (values.length == 1) {
            Mockito.when(mockIterator.hasNext()).thenReturn(true, false);
            Mockito.when(mockIterator.next()).thenReturn(values[0]);
        } else {
            // build boolean array for hasNext()
            Boolean[] hasNextResponses = new Boolean[values.length];
            for (int i = 0; i < hasNextResponses.length -1 ; i++) {
                hasNextResponses[i] = true;
            }
            hasNextResponses[hasNextResponses.length - 1] = false;
            Mockito.when(mockIterator.hasNext()).thenReturn(true, hasNextResponses);
            T[] valuesMinusTheFirst = Arrays.copyOfRange(values, 1, values.length);
            Mockito.when(mockIterator.next()).thenReturn(values[0], valuesMinusTheFirst);
        }
    }
}
