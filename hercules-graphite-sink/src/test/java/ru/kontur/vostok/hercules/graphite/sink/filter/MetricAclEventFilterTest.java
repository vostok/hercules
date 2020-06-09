package ru.kontur.vostok.hercules.graphite.sink.filter;

import org.junit.Assert;
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
public class MetricAclEventFilterTest {

    @Test
    public void metricAclEventFilterTest() {
        final Properties properties = new Properties();
        properties.setProperty("acl.path", "file://src/test/resources/metrics.acl");
        properties.setProperty("acl.defaultStatement", "PERMIT");
        final MetricAclEventFilter filter = new MetricAclEventFilter(properties);

        Event event = EventBuilder.create(0, UUID.randomUUID())
                .tag(MetricsTags.TAGS_VECTOR_TAG.getName(), Variant.ofVector(Vector.ofContainers(
                        Container.builder()
                                .tag(MetricsTags.TAG_KEY_TAG.getName(), Variant.ofString("key"))
                                .tag(MetricsTags.TAG_VALUE_TAG.getName(), Variant.ofString("test"))
                                .build(),
                        Container.builder()
                                .tag(MetricsTags.TAG_KEY_TAG.getName(), Variant.ofString("second_key"))
                                .tag(MetricsTags.TAG_VALUE_TAG.getName(), Variant.ofString("foo"))
                                .build(),
                        Container.builder()
                                .tag(MetricsTags.TAG_KEY_TAG.getName(), Variant.ofString("third_key"))
                                .tag(MetricsTags.TAG_VALUE_TAG.getName(), Variant.ofString("bar"))
                                .build(),
                        Container.builder()
                                .tag(MetricsTags.TAG_KEY_TAG.getName(), Variant.ofString("third_key"))
                                .tag(MetricsTags.TAG_VALUE_TAG.getName(), Variant.ofString("any"))
                                .build()
                )))
                .tag(MetricsTags.METRIC_VALUE_TAG.getName(), Variant.ofDouble(1.5d))
                .build();

        Assert.assertFalse("Suitable for first ace from metrics.acl", filter.test(event));


        event = EventBuilder.create(0, UUID.randomUUID())
                .tag(MetricsTags.TAGS_VECTOR_TAG.getName(), Variant.ofVector(Vector.ofContainers(
                        Container.builder()
                                .tag(MetricsTags.TAG_KEY_TAG.getName(), Variant.ofString("key"))
                                .tag(MetricsTags.TAG_VALUE_TAG.getName(), Variant.ofString("test"))
                                .build(),
                        Container.builder()
                                .tag(MetricsTags.TAG_KEY_TAG.getName(), Variant.ofString("second_key"))
                                .tag(MetricsTags.TAG_VALUE_TAG.getName(), Variant.ofString("foo"))
                                .build(),
                        Container.builder()
                                .tag(MetricsTags.TAG_KEY_TAG.getName(), Variant.ofString("third_key"))
                                .tag(MetricsTags.TAG_VALUE_TAG.getName(), Variant.ofString("any"))
                                .build()
                )))
                .tag(MetricsTags.METRIC_VALUE_TAG.getName(), Variant.ofDouble(1.5d))
                .build();

        Assert.assertTrue("Suitable for second ace from metrics.acl", filter.test(event));


        event = EventBuilder.create(0, UUID.randomUUID())
                .tag(MetricsTags.TAGS_VECTOR_TAG.getName(), Variant.ofVector(Vector.ofContainers(
                        Container.builder()
                                .tag(MetricsTags.TAG_KEY_TAG.getName(), Variant.ofString("key"))
                                .tag(MetricsTags.TAG_VALUE_TAG.getName(), Variant.ofString("test"))
                                .build(),
                        Container.builder()
                                .tag(MetricsTags.TAG_KEY_TAG.getName(), Variant.ofString("third_key"))
                                .tag(MetricsTags.TAG_VALUE_TAG.getName(), Variant.ofString("any"))
                                .build()
                )))
                .tag(MetricsTags.METRIC_VALUE_TAG.getName(), Variant.ofDouble(1.5d))
                .build();

        Assert.assertFalse("Suitable for third ace from metrics.acl", filter.test(event));


        event = EventBuilder.create(0, UUID.randomUUID())
                .tag(MetricsTags.TAGS_VECTOR_TAG.getName(), Variant.ofVector(Vector.ofContainers(
                        Container.builder()
                                .tag(MetricsTags.TAG_KEY_TAG.getName(), Variant.ofString("third_key"))
                                .tag(MetricsTags.TAG_VALUE_TAG.getName(), Variant.ofString("any"))
                                .build()
                )))
                .tag(MetricsTags.METRIC_VALUE_TAG.getName(), Variant.ofDouble(1.5d))
                .build();

        Assert.assertTrue("Return default statement", filter.test(event));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfAclFileWasNotFound() {
        final Properties properties = new Properties();
        properties.setProperty("acl.path", "file://src/test/resources/otherMetrics.acl");
        properties.setProperty("acl.defaultStatement", "PERMIT");
        new MetricAclEventFilter(properties);
    }
}
