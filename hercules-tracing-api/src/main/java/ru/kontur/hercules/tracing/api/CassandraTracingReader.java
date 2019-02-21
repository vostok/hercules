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
import ru.kontur.vostok.hercules.util.throwable.NotImplementedException;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
     * @param count count of events
     * @param pagingStateString page state returned in previous fetch or null on first fetch
     * @return list of events
     */
    public PagedResult<Event> getTraceSpansByTraceId(
        @NotNull final UUID traceId,
        final int count,
        @Nullable final String pagingStateString
    ) {
        final SimpleStatement simpleStatement = new SimpleStatement(
            "SELECT payload FROM tracing_spans where trace_id = ?",
            traceId
        );
        simpleStatement.setFetchSize(count);
        if (Objects.nonNull(pagingStateString)) {
            simpleStatement.setPagingState(PagingState.fromString(pagingStateString));
        }

        final ResultSet resultSet = session.execute(simpleStatement);

        int remaining = resultSet.getAvailableWithoutFetching();
        final List<Event> events = new ArrayList<>(remaining);
        for (Row row : resultSet) {
            events.add(convert(row));

            // Cassandra will fetch next records on iteration, so we need to prevent this behavior
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

    public PagedResult<Event> getTraceSpansByTraceIdAndParentSpanId(final UUID traceId, final UUID parentSpanId, final PageInfo pageInfo) {
        throw new NotImplementedException();
    }

    public Optional<Event> getTraceSpanByTraceIdAndSpanId(final UUID traceUd, final UUID spanId) {
        throw new NotImplementedException();
    }

    private static Event convert(final Row row) {
        final ByteBuffer payload = row.getBytes(0);
        return EVENT_READER.read(new Decoder(payload.array()));
    }
}
