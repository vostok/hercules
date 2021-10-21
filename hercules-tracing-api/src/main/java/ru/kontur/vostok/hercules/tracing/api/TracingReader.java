package ru.kontur.vostok.hercules.tracing.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.tracing.api.cassandra.CassandraTracingReader;
import ru.kontur.vostok.hercules.tracing.api.clickhouse.ClickHouseTracingReader;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.io.Closeable;
import java.util.Properties;
import java.util.UUID;

/**
 * @author Gregory Koshelev
 */
public interface TracingReader extends Closeable {
    /**
     * Get trace spans by trace id.
     *
     * @param traceId     trace id
     * @param limit       max count of fetched spans
     * @param pagingState page state returned in previous fetch or {@code null} on first fetch
     * @return page of trace spans
     */
    Page<Event> getTraceSpansByTraceId(
            @NotNull UUID traceId,
            int limit,
            @Nullable String pagingState);

    /**
     * Get trace spans by trace id and parent span id.
     *
     * @param traceId      trace id
     * @param parentSpanId parent span id
     * @param limit        max count of fetched spans
     * @param pagingState  page state returned in previous fetch or {@code null} on first fetch
     * @return page of trace spans
     */
    Page<Event> getTraceSpansByTraceIdAndParentSpanId(
            @NotNull UUID traceId,
            @NotNull UUID parentSpanId,
            int limit,
            @Nullable String pagingState);

    static TracingReader createTracingReader(Properties properties) {
        Source source = PropertiesUtil.get(Props.SOURCE, properties).get();

        switch (source) {
            case CASSANDRA:
                return new CassandraTracingReader(properties);
            case CLICKHOUSE:
                return new ClickHouseTracingReader(properties);
            default:
                throw new IllegalArgumentException("Unknown source " + source);
        }
    }

    class Props {
        static final Parameter<Source> SOURCE =
                Parameter.enumParameter("source", Source.class).
                        withDefault(Source.CASSANDRA).
                        build();
    }
}
