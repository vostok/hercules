package ru.kontur.vostok.hercules.opentelemetry.adapter.converters;

import com.google.protobuf.ByteString;
import io.opentelemetry.proto.resource.v1.Resource;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import org.jetbrains.annotations.NotNull;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventBuilder;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.tags.TraceSpanTags;
import ru.kontur.vostok.hercules.util.time.TimeUtil;
import ru.kontur.vostok.hercules.uuid.UuidGenerator;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Trace converter is used to convert OpenTelemetry ResourceSpans to Hercules events.
 *
 * @author Innokentiy Krivonosov
 * @see <a href="https://github.com/open-telemetry/opentelemetry-proto/blob/main/opentelemetry/proto/collector/trace/v1/trace_service.proto">
 * OpenTelemetry trace_service.proto</a>
 */
public class TraceConverter {
    public static List<Event> convert(ResourceSpans resourceSpans) {
        Resource resource = resourceSpans.getResource();

        return resourceSpans
                .getScopeSpansList()
                .stream()
                .flatMap(it -> convertSpans(it, resource))
                .collect(Collectors.toList());
    }

    private static Stream<Event> convertSpans(ScopeSpans scopeSpans, Resource resource) {
        return scopeSpans
                .getSpansList()
                .stream()
                .map(it -> convertSpan(it, resource));
    }

    private static Event convertSpan(Span span, Resource resource) {
        long beginTimestamp = TimeUtil.nanosToTicks(span.getStartTimeUnixNano());
        long endTimestamp = TimeUtil.nanosToTicks(span.getEndTimeUnixNano());

        Container annotations = TraceAnnotationsConverter.getAnnotations(span, resource);

        EventBuilder eventBuilder = EventBuilder.create()
                .uuid(UuidGenerator.getClientInstance().next())
                .timestamp(endTimestamp)
                .tag(TraceSpanTags.TRACE_ID_TAG.getName(), Variant.ofUuid(getTraceId(span.getTraceId())))
                .tag(TraceSpanTags.SPAN_ID_TAG.getName(), Variant.ofUuid(getSpanId(span.getSpanId())))
                .tag(TraceSpanTags.BEGIN_TIMESTAMP_UTC_TAG.getName(), Variant.ofLong(beginTimestamp))
                .tag(TraceSpanTags.BEGIN_TIMESTAMP_UTC_OFFSET_TAG.getName(), Variant.ofLong(0L))
                .tag(TraceSpanTags.END_TIMESTAMP_UTC_TAG.getName(), Variant.ofLong(endTimestamp))
                .tag(TraceSpanTags.END_TIMESTAMP_UTC_OFFSET_TAG.getName(), Variant.ofLong(0L))
                .tag(TraceSpanTags.ANNOTATIONS_TAG.getName(), Variant.ofContainer(annotations));

        if (!span.getParentSpanId().isEmpty()) {
            eventBuilder.tag(
                    TraceSpanTags.PARENT_SPAN_ID_TAG.getName(),
                    Variant.ofUuid(getSpanId(span.getParentSpanId()))
            );
        }

        return eventBuilder.build();
    }

    @NotNull
    private static UUID getTraceId(ByteString bytes) {
        ByteBuffer byteBuffer = bytes.asReadOnlyByteBuffer();
        return new UUID(byteBuffer.getLong(), byteBuffer.getLong());
    }

    @NotNull
    protected static UUID getSpanId(ByteString bytes) {
        return new UUID(bytes.asReadOnlyByteBuffer().getLong(), 0);
    }
}
