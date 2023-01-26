package ru.kontur.vostok.hercules.opentelemetry.adapter.converters;

import com.google.protobuf.ByteString;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.resource.v1.Resource;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.hpath.HPath;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static ru.kontur.vostok.hercules.opentelemetry.adapter.converters.TraceConverter.getSpanId;

/**
 * @author Innokentiy Krivonosov
 */
public class TraceConverterTest {

    @Test
    public void base() {
        long spanId = new Random().nextLong();
        long parentSpanId = new Random().nextLong();
        UUID traceId = UUID.randomUUID();
        Instant start = Instant.now();
        Instant end = start.plus(Duration.ofSeconds(1));

        Span span = Span.newBuilder()
                .setKind(Span.SpanKind.SPAN_KIND_CLIENT)
                .setSpanId(asBytes(spanId))
                .setParentSpanId(asBytes(parentSpanId))
                .setTraceId(asBytes(traceId))
                .setStartTimeUnixNano(start.getNano())
                .setEndTimeUnixNano(end.getNano())
                .build();

        ScopeSpans librarySpans = ScopeSpans.newBuilder().addSpans(span).build();

        Resource resource = Resource.newBuilder()
                .addAttributes(getStringValueAttr("service.name", "test-app"))
                .build();

        ResourceSpans resourceSpans = ResourceSpans.newBuilder()
                .addScopeSpans(librarySpans)
                .setResource(resource)
                .build();

        List<Event> events = TraceConverter.convert(resourceSpans);
        Event event = events.get(0);

        Container payload = event.getPayload();

        assertEquals("(UUID) " + traceId, HPath.fromPath("traceId").extract(payload).toString());
        assertEquals("(UUID) " + getSpanId(asBytes(spanId)), HPath.fromPath("spanId").extract(payload).toString());
        assertEquals("(UUID) " + getSpanId(asBytes(parentSpanId)), HPath.fromPath("parentSpanId").extract(payload).toString());

        assertEquals("(LONG) " + TimeUtil.nanosToTicks(start.getNano()), HPath.fromPath("beginTimestampUtc").extract(payload).toString());
        assertEquals("(LONG) " + TimeUtil.nanosToTicks(end.getNano()), HPath.fromPath("endTimestampUtc").extract(payload).toString());

        assertEquals("(STRING) http-request-client", HPath.fromPath("annotations/kind").extract(payload).toString());
        assertEquals("(STRING) test-app", HPath.fromPath("annotations/application").extract(payload).toString());
    }

    @NotNull
    public static KeyValue getStringValueAttr(String key, String value) {
        return KeyValue.newBuilder().setKey(key)
                .setValue(AnyValue.newBuilder().setStringValue(value).build())
                .build();
    }

    @NotNull
    public static KeyValue getLongValueAttr(String key, long value) {
        return KeyValue.newBuilder().setKey(key)
                .setValue(AnyValue.newBuilder().setIntValue(value).build())
                .build();
    }

    public static ByteString asBytes(UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return ByteString.copyFrom(bb.array());
    }

    public static ByteString asBytes(long value) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[8]);
        bb.putLong(value);
        return ByteString.copyFrom(bb.array());
    }
}