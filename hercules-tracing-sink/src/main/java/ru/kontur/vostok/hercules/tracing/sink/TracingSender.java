package ru.kontur.vostok.hercules.tracing.sink;

import ru.kontur.vostok.hercules.cassandra.sink.CassandraSender;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.tags.TraceSpanTags;
import ru.kontur.vostok.hercules.util.ObjectUtil;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

/**
 * Sender inserts traces into traces table in Cassandra.
 *
 * @author Gregory Koshelev
 * @see CassandraSender
 */
public class TracingSender extends CassandraSender {

    private final String tableName;

    public TracingSender(Properties properties, MetricsCollector metricsCollector) {
        super(properties, metricsCollector);

        tableName = Props.TABLE_NAME.extract(properties);
    }

    @Override
    protected String query() {
        return "INSERT INTO " + tableName + " (" +
                "  trace_id," +
                "  parent_span_id," +
                "  span_id," +
                "  payload" +
                ") " +
                "VALUES (?, ?, ?, ?)";
    }

    @Override
    protected Optional<Object[]> convert(Event event) {
        final Optional<UUID> traceId = ContainerUtil.extract(event.getPayload(), TraceSpanTags.TRACE_ID_TAG);
        final Optional<UUID> spanId = ContainerUtil.extract(event.getPayload(), TraceSpanTags.SPAN_ID_TAG);

        if (!traceId.isPresent() || !spanId.isPresent()) {
            return Optional.empty();
        }

        final Optional<UUID> parentSpanId = ContainerUtil.extract(event.getPayload(), TraceSpanTags.PARENT_SPAN_ID_TAG);
        final ByteBuffer payload = ByteBuffer.wrap(event.getBytes());

        return Optional.of(new Object[]{
                traceId.get(),
                ObjectUtil.nullToNilUuidValue(parentSpanId.orElse(null)),
                spanId.get(),
                payload
        });
    }

    private static class Props {
        static final PropertyDescription<String> TABLE_NAME =
                PropertyDescriptions.stringProperty("tableName").
                        withDefaultValue("tracing_spans").
                        build();
    }
}
