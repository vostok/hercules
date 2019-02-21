package ru.kontur.vostok.hercules.tags;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.util.TagDescription;
import ru.kontur.vostok.hercules.protocol.util.TagDescriptionBuilder;

import java.util.Optional;
import java.util.UUID;

/**
 * TraceSpanTags
 *
 * @author Kirill Sulim
 */
public final class TraceSpanTags {

    /**
     * Trace id tag
     */
    public static final TagDescription<Optional<UUID>> TRACE_ID_TAG = TagDescriptionBuilder
        .uuid("traceId")
        .optional()
        .build();

    /**
     * Span id tag
     */
    public static final TagDescription<Optional<UUID>> SPAN_ID_TAG = TagDescriptionBuilder
        .uuid("spanId")
        .optional()
        .build();

    /**
     * Parent span id
     */
    public static final TagDescription<Optional<UUID>> PARENT_SPAN_ID_TAG = TagDescriptionBuilder
        .uuid("parentSpanId")
        .optional()
        .build();

    /**
     * Begin timestamp in 100ns ticks of UNIX-epoch
     */
    public static final TagDescription<Optional<Long>> BEGIN_TIMESTAMP_UTC_TAG = TagDescriptionBuilder
        .longTag("beginTimestampUtc")
        .optional()
        .build();

    /**
     * Begin timestamp offset from UTC in 100ns ticks
     */
    public static final TagDescription<Optional<Long>> BEGIN_TIMESTAMP_UTC_OFFSET_TAG = TagDescriptionBuilder
        .longTag("beginTimestampUtcOffset")
        .optional()
        .build();

    /**
     * End timestamp in 100ns ticks of UNIX-epoch
     */
    public static final TagDescription<Optional<Long>> END_TIMESTAMP_UTC_TAG = TagDescriptionBuilder
        .longTag("endTimestampUtc")
        .optional()
        .build();

    /**
     * End timestamp offset from UTC in 100ns ticks
     */
    public static final TagDescription<Optional<Long>> END_TIMESTAMP_UTC_OFFSET_TAG = TagDescriptionBuilder
        .longTag("endTimestampUtcOffset")
        .optional()
        .build();

    /**
     * Annotations container tag
     */
    public static final TagDescription<Optional<Container>> ANNOTATIONS_TAG = TagDescriptionBuilder
        .container("annotations")
        .optional()
        .build();

    private TraceSpanTags() {
        /* static class */
    }
}
