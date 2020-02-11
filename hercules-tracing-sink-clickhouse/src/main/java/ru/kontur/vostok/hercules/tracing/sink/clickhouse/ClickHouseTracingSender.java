package ru.kontur.vostok.hercules.tracing.sink.clickhouse;

import ru.kontur.vostok.hercules.clickhouse.sink.ClickHouseSender;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.tags.TraceSpanTags;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.yandex.clickhouse.util.ClickHouseRowBinaryStream;

import java.io.IOException;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

/**
 * @author Gregory Koshelev
 */
public class ClickHouseTracingSender extends ClickHouseSender {
    private static final UUID DEFAULT_UUID = new UUID(0,0);

    private final String query;

    public ClickHouseTracingSender(Properties properties, MetricsCollector metricsCollector) {
        super(properties, metricsCollector);

        String tableName = PropertiesUtil.get(Props.TABLE_NAME, properties).get();
        this.query = "INSERT INTO " + tableName + " (trace_id, parent_span_id, span_id, payload)";
    }

    @Override
    protected String query() {
        return query;
    }

    @Override
    protected boolean write(ClickHouseRowBinaryStream stream, Event event) throws IOException {
        final Optional<UUID> traceId = ContainerUtil.extract(event.getPayload(), TraceSpanTags.TRACE_ID_TAG);
        final Optional<UUID> spanId = ContainerUtil.extract(event.getPayload(), TraceSpanTags.SPAN_ID_TAG);

        if (!traceId.isPresent() || !spanId.isPresent()) {
            return false;
        }

        final Optional<UUID> parentSpanId = ContainerUtil.extract(event.getPayload(), TraceSpanTags.PARENT_SPAN_ID_TAG);

        stream.writeUUID(traceId.get());
        stream.writeUUID(parentSpanId.orElse(DEFAULT_UUID));
        stream.writeUUID(spanId.get());
        stream.writeUnsignedLeb128(event.getBytes().length);
        stream.writeBytes(event.getBytes());

        return true;
    }

    private static class Props {
        static final Parameter<String> TABLE_NAME =
                Parameter.stringParameter("tableName").
                        withDefault("tracing_spans").
                        build();
    }
}
