package ru.kontur.vostok.hercules.radamant;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.util.UUID;
import org.junit.Before;
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
public class TopicNameGeneratorTest {
    private TopicNameGenerator nameGenerator;

    private final String DEFAULT_TOPIC_NAME = "defaultTopic";

    @Before
    public void init() {
        nameGenerator = new TopicNameGenerator(DEFAULT_TOPIC_NAME);
    }

    @Test
    public void shouldReturnDefaultIfEventNotContainsTopicName() {
        Event event = EventBuilder.create(TimeUtil.millisToTicks(Instant.now().toEpochMilli()),
                        UUID.randomUUID())
                .tag(MetricsTags.TAGS_VECTOR_TAG.getName(), Variant.ofVector(Vector.ofContainers(
                        Container.builder()
                                .tag(MetricsTags.TAG_KEY_TAG.getName(), Variant.ofString("_name"))
                                .tag(MetricsTags.TAG_VALUE_TAG.getName(),
                                        Variant.ofString("foo.bar.baz"))
                                .build()
                )))
                .tag(MetricsTags.METRIC_VALUE_TAG.getName(), Variant.ofDouble(42))
                .build();
        assertEquals(DEFAULT_TOPIC_NAME, nameGenerator.extract(null, event, null));
    }

    @Test
    public void shouldReturnTopicNameIfEventContainsTopicName() {
        String CUSTOM_TOPIC_NAME = "customTopic";
        Event event = EventBuilder.create(TimeUtil.millisToTicks(Instant.now().toEpochMilli()),
                        UUID.randomUUID())
                .tag(MetricsTags.TAGS_VECTOR_TAG.getName(), Variant.ofVector(Vector.ofContainers(
                        Container.builder()
                                .tag(MetricsTags.TAG_KEY_TAG.getName(), Variant.ofString("_name"))
                                .tag(MetricsTags.TAG_VALUE_TAG.getName(),
                                        Variant.ofString("foo.bar.baz"))
                                .build()
                )))
                .tag(MetricsTags.METRIC_VALUE_TAG.getName(), Variant.ofDouble(42))
                .tag(MetricsTags.ENRICHMENT_TAG.getName(), Variant.ofContainer(
                        Container.builder()
                                .tag("topic", Variant.ofString(CUSTOM_TOPIC_NAME))
                                .build()
                ))
                .build();
        assertEquals(CUSTOM_TOPIC_NAME, nameGenerator.extract(null, event, null));
    }
}
