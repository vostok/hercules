package ru.kontur.vostok.hercules.tracing.api.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatementBuilder;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.kontur.vostok.hercules.cassandra.util.CassandraConnector;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.decoder.Decoder;
import ru.kontur.vostok.hercules.protocol.decoder.EventReader;
import ru.kontur.vostok.hercules.tracing.api.Page;
import ru.kontur.vostok.hercules.tracing.api.TracingReader;
import ru.kontur.vostok.hercules.util.bytes.ByteUtil;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

/**
 * @author Gregory Koshelev
 */
public class CassandraTracingReader implements TracingReader {
    private static final EventReader EVENT_READER = EventReader.readAllTags();

    private final CassandraConnector connector;
    private final CqlSession session;

    private final PreparedStatement selectTraceSpansByTraceIdQuery;
    private final PreparedStatement selectTraceSpansByTraceIdAndParentSpanIdQuery;

    public CassandraTracingReader(Properties properties) {
        Properties cassandraProperties = PropertiesUtil.ofScope(properties, Scopes.CASSANDRA);

        this.connector = new CassandraConnector(cassandraProperties);
        this.session = connector.session();

        String table = PropertiesUtil.get(Props.TABLE, properties).get();

        this.selectTraceSpansByTraceIdQuery = session.prepare(
                "SELECT payload" +
                        " FROM " + table +
                        " WHERE trace_id = ?");
        this.selectTraceSpansByTraceIdAndParentSpanIdQuery = session.prepare(
                "SELECT payload" +
                        " FROM " + table +
                        " WHERE trace_id = ? AND parent_span_id = ?");
    }

    /**
     * Get trace spans by trace id
     *
     * @param traceId     trace id
     * @param limit       max count of fetched events
     * @param pagingState page state returned in previous fetch or null on first fetch
     * @return list of events
     */
    @Override
    public Page<Event> getTraceSpansByTraceId(
            @NotNull final UUID traceId,
            final int limit,
            @Nullable final String pagingState) {
        return select(
                selectTraceSpansByTraceIdQuery,
                limit,
                pagingState,
                traceId
        );
    }

    /**
     * Get trace spans by span id and parent span id
     *
     * @param traceId      trace id
     * @param parentSpanId parent span id
     * @param limit        max count of fetched events
     * @param pagingState  page state returned in previous fetch or null on first fetch
     * @return list of events
     */
    @Override
    public Page<Event> getTraceSpansByTraceIdAndParentSpanId(
            @NotNull final UUID traceId,
            @NotNull final UUID parentSpanId,
            final int limit,
            @Nullable final String pagingState) {
        return select(
                selectTraceSpansByTraceIdAndParentSpanIdQuery,
                limit,
                pagingState,
                traceId,
                parentSpanId);
    }

    @Override
    public void close() {
        connector.close();
    }

    private Page<Event> select(
            @NotNull final PreparedStatement selectQuery,
            final int limit,
            @Nullable final String pagingStateString,
            Object... params) {

        BoundStatementBuilder statementBuilder = selectQuery.boundStatementBuilder(params);

        statementBuilder.setPageSize(limit);

        if (pagingStateString != null) {
            statementBuilder.setPagingState(ByteBuffer.wrap(ByteUtil.fromHexString(pagingStateString)));
        }

        final ResultSet resultSet = session.execute(statementBuilder.build());

        // https://docs.datastax.com/en/developer/java-driver/4.0/manual/core/paging/
        //
        // Quote from the docs:
        // > Note that the page size is merely a hint;
        // > the server will not always return the exact number of rows,
        // > it might decide to return slightly more or less.
        //
        // FIXME: Change this logic if this behavior is unexpected (suppose client sees fewer records and thinks that no more records)
        //
        // Cassandra will fetch next records on iteration, so we need to forcibly break the iteration
        int remaining = Math.min(resultSet.getAvailableWithoutFetching(), limit);
        final List<Event> events = new ArrayList<>(remaining);
        for (Row row : resultSet) {
            events.add(convert(row));

            // Forcibly break the iteration
            if (--remaining == 0) {
                break;
            }
        }

        final ByteBuffer pagingState = resultSet.getExecutionInfo().getPagingState();

        return new Page<>(
                events,
                pagingState != null ? ByteUtil.toHexString(pagingState) : null);
    }

    private static Event convert(final Row row) {
        final ByteBuffer payload = row.getByteBuffer(0);
        return EVENT_READER.read(new Decoder(payload));
    }

    private static class Props {
        static Parameter<String> TABLE =
                Parameter.stringParameter("table").
                        withDefault("tracing_spans").
                        build();
    }
}
