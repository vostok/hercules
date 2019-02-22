package ru.kontur.hercules.tracing.api;

import com.datastax.driver.core.PagingState;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleStatement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.kontur.hercules.tracing.api.cassandra.PagedResult;
import ru.kontur.vostok.hercules.cassandra.util.CassandraConnector;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.decoder.Decoder;
import ru.kontur.vostok.hercules.protocol.decoder.EventReader;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * CassandraTracingReader
 *
 * @author Kirill Sulim
 */
public class CassandraTracingReader {

    private static final EventReader EVENT_READER = EventReader.readAllTags();

    private final CassandraConnector cassandraConnector;
    private final Session session;

    public CassandraTracingReader(CassandraConnector cassandraConnector) {
        this.cassandraConnector = cassandraConnector;
        this.session = cassandraConnector.session();
    }

    /**
     * Get trace spans by trace id
     *
     * @param traceId trace id
     * @param limit max count of fetched events
     * @param pagingStateString page state returned in previous fetch or null on first fetch
     * @return list of events
     */
    public PagedResult<Event> getTraceSpansByTraceId(
        @NotNull final UUID traceId,
        final int limit,
        @Nullable final String pagingStateString
    ) {
        return select(
            "SELECT payload FROM tracing_spans WHERE trace_id = ?",
            limit,
            pagingStateString,
            traceId
        );
    }

    /**
     * Get trace spans by span id and parent span id
     *
     * @param traceId trace id
     * @param parentSpanId parent span id
     * @param limit max count of fetched events
     * @param pagingStateString page state returned in previous fetch or null on first fetch
     * @return list of events
     */
    public PagedResult<Event> getTraceSpansByTraceIdAndParentSpanId(
        @NotNull final UUID traceId,
        @NotNull final UUID parentSpanId,
        final int limit,
        @Nullable final String pagingStateString
    ) {
        return select(
            "SELECT payload FROM tracing_spans WHERE trace_id = ? AND parent_span_id = ?",
            limit,
            pagingStateString,
            traceId,
            parentSpanId
        );
    }

    private PagedResult<Event> select(
        @NotNull final String selectRequest,
        final int limit,
        @Nullable final String pagingStateString,
        Object ... params
    ) {

        final SimpleStatement simpleStatement = new SimpleStatement(
            selectRequest,
            params
        );

        simpleStatement.setFetchSize(limit);
        if (Objects.nonNull(pagingStateString)) {
            simpleStatement.setPagingState(PagingState.fromString(pagingStateString));
        }

        final ResultSet resultSet = session.execute(simpleStatement);

        // Cassandra will fetch next records on iteration, so we need to forcibly break the iteration
        int remaining = resultSet.getAvailableWithoutFetching();
        final List<Event> events = new ArrayList<>(remaining);
        for (Row row : resultSet) {
            events.add(convert(row));

            // Forcibly break the iteration
            if (--remaining == 0) {
                break;
            }
        }

        final PagingState pagingState = resultSet.getExecutionInfo().getPagingState();

        return new PagedResult<>(
            events,
            Objects.nonNull(pagingState) ? pagingState.toString() : null
        );
    }

    private static Event convert(final Row row) {
        final ByteBuffer payload = row.getBytes(0);
        return EVENT_READER.read(new Decoder(payload.array()));
    }
}
