package ru.kontur.vostok.hercules.radamant;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.kontur.vostok.hercules.radamant.MetricsUtil.isFlatMetric;

import java.time.Instant;
import java.util.UUID;
import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventBuilder;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.Vector;
import ru.kontur.vostok.hercules.tags.MetricsTags;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

/**
 * @author Tatyana Tokmyanina
 */
public class EventFiltersTest {
    @Test
    public void shouldReturnTrueIfMetricIsFlat() {
        Event flatMetricEvent = EventBuilder.create(
                        TimeUtil.millisToTicks(Instant.now().toEpochMilli()), UUID.randomUUID())
                .tag(MetricsTags.TAGS_VECTOR_TAG.getName(), Variant.ofVector(Vector.ofContainers(
                        Container.builder()
                                .tag(MetricsTags.TAG_KEY_TAG.getName(), Variant.ofString("_name"))
                                .tag(MetricsTags.TAG_VALUE_TAG.getName(),
                                        Variant.ofString("foo.bar.baz"))
                                .build()
                )))
                .tag(MetricsTags.METRIC_VALUE_TAG.getName(), Variant.ofDouble(42))
                .build();
        assertTrue(isFlatMetric(flatMetricEvent));
    }

    @Test
    public void shouldReturnFalseIfMetricIsTagged() {
        Event taggedMetricEvent = EventBuilder.create(
                        TimeUtil.millisToTicks(Instant.now().toEpochMilli()), UUID.randomUUID())
                .tag(MetricsTags.TAGS_VECTOR_TAG.getName(), Variant.ofVector(Vector.ofContainers(
                        Container.builder()
                                .tag(MetricsTags.TAG_KEY_TAG.getName(), Variant.ofString("foo"))
                                .tag(MetricsTags.TAG_VALUE_TAG.getName(),
                                        Variant.ofString("foobar"))
                                .build(),
                        Container.builder()
                                .tag(MetricsTags.TAG_KEY_TAG.getName(), Variant.ofString("bar"))
                                .tag(MetricsTags.TAG_VALUE_TAG.getName(),
                                        Variant.ofString("foobar"))
                                .build()
                )))
                .tag(MetricsTags.METRIC_VALUE_TAG.getName(), Variant.ofDouble(42))
                .build();
        assertFalse(isFlatMetric(taggedMetricEvent));
    }
}
