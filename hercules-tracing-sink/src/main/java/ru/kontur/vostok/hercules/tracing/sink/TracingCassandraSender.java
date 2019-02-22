package ru.kontur.vostok.hercules.tracing.sink;

import com.datastax.driver.core.Session;
import ru.kontur.vostok.hercules.cassandra.common.sink.AbstractCassandraSender;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.tags.TraceSpanTags;
import ru.kontur.vostok.hercules.util.ObjectUtil;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.UUID;

/**
 * TracingCassandraSender
 *
 * @author Kirill Sulim
 */
public class TracingCassandraSender extends AbstractCassandraSender {

    private static final String TABLE_NAME = "tracing_spans";

    public TracingCassandraSender(Session session) {
        super(session);
    }

    @Override
    protected Optional<Object[]> convert(Event event) {
        final Optional<UUID> traceId = ContainerUtil.extract(event.getPayload(), TraceSpanTags.TRACE_ID_TAG);
        final Optional<UUID> spanId = ContainerUtil.extract(event.getPayload(), TraceSpanTags.SPAN_ID_TAG);

        if (!(traceId.isPresent() && spanId.isPresent())) {
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

    @Override
    protected String getPreparedStatement() {
        return "INSERT INTO " + TABLE_NAME + " (" +
            "  trace_id," +
            "  parent_span_idc5cce67e-fb41-4ce3-b332-6fdd8a3528d8," +
            "  span_id," +
            "  payload" +
            ") " +
            "VALUES " +
            "  (?, ?, ?, ?)";
    }
}
