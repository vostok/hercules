package ru.kontur.vostok.hercules.tracing.api.clickhouse;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.clickhouse.util.ClickHouseConnector;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.decoder.Decoder;
import ru.kontur.vostok.hercules.protocol.decoder.EventReader;
import ru.kontur.vostok.hercules.tracing.api.Page;
import ru.kontur.vostok.hercules.tracing.api.TracingReader;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.text.StringUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

/**
 * @author Gregory Koshelev
 */
public class ClickHouseTracingReader implements TracingReader {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClickHouseTracingReader.class);

    private static final EventReader EVENT_READER = EventReader.readAllTags();

    private final ClickHouseConnector connector;
    private final String selectByTraceIdQuery;
    private final String selectByTraceIdAndParentSpanIdQuery;

    public ClickHouseTracingReader(Properties properties) {
        Properties clickHouseProperties = PropertiesUtil.ofScope(properties, Scopes.CLICKHOUSE);

        this.connector = new ClickHouseConnector(clickHouseProperties);

        String table = PropertiesUtil.get(Props.TABLE, properties).get();

        this.selectByTraceIdQuery = "SELECT payload" +
                " FROM " + table +
                " WHERE trace_id = ?" +
                " ORDER BY parent_span_id, span_id" +
                " LIMIT ? OFFSET ?";
        this.selectByTraceIdAndParentSpanIdQuery = "SELECT payload" +
                " FROM " + table +
                " WHERE trace_id = ? AND parent_span_id = ?" +
                " ORDER BY span_id" +
                " LIMIT ? OFFSET ?";
    }

    @Override
    public Page<Event> getTraceSpansByTraceId(@NotNull UUID traceId, int limit, @Nullable String pagingState) {
        long offset = pagingStateToOffset(pagingState);
        return select(selectByTraceIdQuery, limit, offset, traceId, limit, offset);
    }

    @Override
    public Page<Event> getTraceSpansByTraceIdAndParentSpanId(@NotNull UUID traceId, @NotNull UUID parentSpanId, int limit, @Nullable String pagingState) {
        long offset = pagingStateToOffset(pagingState);
        return select(selectByTraceIdAndParentSpanIdQuery, limit, offset, traceId, parentSpanId, limit, offset);
    }
    @Override
    public void close() {
        connector.close();
    }

    private Page<Event> select(String sql, int limit, long offset, Object... params) {
        Optional<Connection> connection = connector.connection();
        try (PreparedStatement select = connection.orElseThrow(IllegalStateException::new).prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                select.setObject(i + 1, params[i]);
            }
            ResultSet resultSet = select.executeQuery();
            int rowCounter = 0;
            List<Event> events = new ArrayList<>(limit);
            while (resultSet.next()) {
                events.add(convert(resultSet.getBytes(1)));
                rowCounter++;
            }
            return new Page<>(events, rowCounter == limit ? offsetToPagingState(offset + limit) : null);
        } catch (SQLException ex) {
            //TODO: Process SQL Exception
            LOGGER.error("Read failed with exception", ex);
            throw new RuntimeException(ex);
        }
    }

    private static long pagingStateToOffset(@Nullable String pagingState) {
        if (StringUtil.isNullOrEmpty(pagingState)) {
            return 0;
        }
        try {
            return Long.parseLong(pagingState);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static String offsetToPagingState(long offset) {
        return String.valueOf(offset);
    }

    private static Event convert(byte[] payload) {
        return EVENT_READER.read(new Decoder(payload));
    }

    private static class Props {
        static Parameter<String> TABLE =
                Parameter.stringParameter("table").
                        withDefault("tracing_spans").
                        build();
    }
}
