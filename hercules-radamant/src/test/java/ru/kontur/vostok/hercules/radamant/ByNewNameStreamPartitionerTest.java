package ru.kontur.vostok.hercules.radamant;

import java.time.Instant;
import java.util.UUID;
import org.junit.Assert;
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
public class ByNewNameStreamPartitionerTest {
    private final ByNewNameStreamPartitioner partitioner = new ByNewNameStreamPartitioner();
    private final UUID key = UUID.randomUUID();

    Event event = EventBuilder.create(
                    TimeUtil.millisToTicks(Instant.now().toEpochMilli()), UUID.randomUUID())
            .tag(MetricsTags.TAGS_VECTOR_TAG.getName(), Variant.ofVector(Vector.ofContainers(
                    Container.builder()
                            .tag(MetricsTags.TAG_KEY_TAG.getName(), Variant.ofString("_name"))
                            .tag(MetricsTags.TAG_VALUE_TAG.getName(),
                                    Variant.ofString("foo.bar.1"))
                            .build()
            )))
            .tag(MetricsTags.METRIC_VALUE_TAG.getName(), Variant.ofDouble(42))
            .tag(MetricsTags.ENRICHMENT_TAG.getName(), Variant.ofContainer(
                    Container.builder()
                            .tag(MetricsTags.NEW_NAME_TAG.getName(), Variant.ofString("foo.bar"))
                            .build()
            ))
            .build();
    Event sameNewNameEvent = EventBuilder.create(
                    TimeUtil.millisToTicks(Instant.now().toEpochMilli()), UUID.randomUUID())
            .tag(MetricsTags.TAGS_VECTOR_TAG.getName(), Variant.ofVector(Vector.ofContainers(
                    Container.builder()
                            .tag(MetricsTags.TAG_KEY_TAG.getName(), Variant.ofString("_name"))
                            .tag(MetricsTags.TAG_VALUE_TAG.getName(),
                                    Variant.ofString("foo.bar.2"))
                            .build()
            )))
            .tag(MetricsTags.METRIC_VALUE_TAG.getName(), Variant.ofDouble(420))
            .tag(MetricsTags.ENRICHMENT_TAG.getName(), Variant.ofContainer(
                    Container.builder()
                            .tag(MetricsTags.NEW_NAME_TAG.getName(), Variant.ofString("foo.bar"))
                            .build()
            ))
            .build();
    Event anotherNewNameEvent = EventBuilder.create(
                    TimeUtil.millisToTicks(Instant.now().toEpochMilli()), UUID.randomUUID())
            .tag(MetricsTags.TAGS_VECTOR_TAG.getName(), Variant.ofVector(Vector.ofContainers(
                    Container.builder()
                            .tag(MetricsTags.TAG_KEY_TAG.getName(), Variant.ofString("_name"))
                            .tag(MetricsTags.TAG_VALUE_TAG.getName(),
                                    Variant.ofString("foo.bar.1"))
                            .build()
            )))
            .tag(MetricsTags.METRIC_VALUE_TAG.getName(), Variant.ofDouble(42))
            .tag(MetricsTags.ENRICHMENT_TAG.getName(), Variant.ofContainer(
                    Container.builder()
                            .tag(MetricsTags.NEW_NAME_TAG.getName(),
                                    Variant.ofString("other_new_name"))
                            .build()
            ))
            .build();

    @Test
    public void shouldReturnSameValuesIfTagsAreSame() {
        String topic = "topic";
        int numPartitions = 100;
        int first = partitioner.partition(topic, key, event, numPartitions);
        int second = partitioner.partition(topic, key, sameNewNameEvent, numPartitions);
        int third = partitioner.partition(topic, key, anotherNewNameEvent, numPartitions);

        Assert.assertEquals(first, second);
        Assert.assertNotEquals(first, third);
    }
}
