package ru.kontur.vostok.hercules.graphite.sink.filter;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventBuilder;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.Vector;
import ru.kontur.vostok.hercules.tags.MetricsTags;

import java.util.Properties;
import java.util.UUID;

/**
 * @author Vladimir Tsypaev
 */
public class MetricEventFilterTest {
    private static MetricEventFilter filter;

    @BeforeClass
    public static void init() {
        filter = new MetricEventFilter(new Properties());
    }

    @Test
    public void shouldPassCorrectEvent() {
        Event event = EventBuilder.create(0, UUID.randomUUID())
                .tag(MetricsTags.METRIC_VALUE_TAG.getName(), Variant.ofDouble(0.5))
                .tag(MetricsTags.TAGS_VECTOR_TAG.getName(), Variant.ofVector(Vector.ofContainers(
                        Container.of("anyTag", Variant.ofString("anyValue"))
                )))
                .build();

        Assert.assertTrue(filter.test(event));
    }

    @Test
    public void shouldDenyEventWithAggregationType() {
        Event event = EventBuilder.create(0, UUID.randomUUID())
                .tag(MetricsTags.AGGREGATION_TYPE_TAG.getName(), Variant.ofString("any"))
                .tag(MetricsTags.METRIC_VALUE_TAG.getName(), Variant.ofDouble(0.5))
                .tag(MetricsTags.TAGS_VECTOR_TAG.getName(), Variant.ofVector(Vector.ofContainers(
                        Container.of("anyTag", Variant.ofString("anyValue"))
                )))
                .build();

        Assert.assertFalse(filter.test(event));
    }

    @Test
    public void shouldDenyEventWithoutMetricValue() {
        Event event = EventBuilder.create(0, UUID.randomUUID())
                .tag(MetricsTags.TAGS_VECTOR_TAG.getName(), Variant.ofVector(Vector.ofContainers(
                        Container.of("anyTag", Variant.ofString("anyValue"))
                )))
                .build();

        Assert.assertFalse(filter.test(event));
    }

    @Test
    public void shouldDenyEventsWithNonNumericMetricValue() {
        Event eventWithNan = EventBuilder.create(0, UUID.randomUUID())
                .tag(MetricsTags.METRIC_VALUE_TAG.getName(), Variant.ofDouble(Double.NaN))
                .tag(MetricsTags.TAGS_VECTOR_TAG.getName(), Variant.ofVector(Vector.ofContainers(
                        Container.of("anyTag", Variant.ofString("anyValue"))
                )))
                .build();

        Assert.assertFalse(filter.test(eventWithNan));

        Event eventWithNegativeInfinity = EventBuilder.create(0, UUID.randomUUID())
                .tag(MetricsTags.METRIC_VALUE_TAG.getName(), Variant.ofDouble(Double.NEGATIVE_INFINITY))
                .tag(MetricsTags.TAGS_VECTOR_TAG.getName(), Variant.ofVector(Vector.ofContainers(
                        Container.of("anyTag", Variant.ofString("anyValue"))
                )))
                .build();

        Assert.assertFalse(filter.test(eventWithNegativeInfinity));

        Event eventWithPositiveInfinity = EventBuilder.create(0, UUID.randomUUID())
                .tag(MetricsTags.METRIC_VALUE_TAG.getName(), Variant.ofDouble(Double.POSITIVE_INFINITY))
                .tag(MetricsTags.TAGS_VECTOR_TAG.getName(), Variant.ofVector(Vector.ofContainers(
                        Container.of("anyTag", Variant.ofString("anyValue"))
                )))
                .build();

        Assert.assertFalse(filter.test(eventWithPositiveInfinity));
    }

    @Test
    public void shouldDenyEventWithoutTagsVector() {
        Event event = EventBuilder.create(0, UUID.randomUUID())
                .tag(MetricsTags.METRIC_VALUE_TAG.getName(), Variant.ofDouble(0.5))
                .build();

        Assert.assertFalse(filter.test(event));
    }

    @Test
    public void shouldDenyEventWithEmptyTagsVector() {
        Event event = EventBuilder.create(0, UUID.randomUUID())
                .tag(MetricsTags.METRIC_VALUE_TAG.getName(), Variant.ofDouble(0.5))
                .tag(MetricsTags.TAGS_VECTOR_TAG.getName(), Variant.ofVector(Vector.ofContainers()))
                .build();

        Assert.assertFalse(filter.test(event));
    }
}
