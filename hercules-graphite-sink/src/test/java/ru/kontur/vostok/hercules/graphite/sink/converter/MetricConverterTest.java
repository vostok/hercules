package ru.kontur.vostok.hercules.graphite.sink.converter;

import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventBuilder;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.Vector;
import ru.kontur.vostok.hercules.tags.MetricsTags;
import ru.kontur.vostok.hercules.util.metrics.GraphiteSanitizer;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * @author Gregory Koshelev
 */
public class MetricConverterTest {
    private final Event metric = EventBuilder.create(TimeUtil.millisToTicks(System.currentTimeMillis()), UUID.randomUUID())
            .tag(MetricsTags.METRIC_VALUE_TAG.getName(), Variant.ofDouble(42.0))
            .tag(MetricsTags.TAGS_VECTOR_TAG.getName(), Variant.ofVector(Vector.ofContainers(
                    Container.builder()
                            .tag(MetricsTags.TAG_KEY_TAG.getName(), Variant.ofString("project"))
                            .tag(MetricsTags.TAG_VALUE_TAG.getName(), Variant.ofString("hercules"))
                            .build(),
                    Container.builder()
                            .tag(MetricsTags.TAG_KEY_TAG.getName(), Variant.ofString("subproject"))
                            .tag(MetricsTags.TAG_VALUE_TAG.getName(), Variant.ofString("graphite-sink"))
                            .build(),
                    Container.builder()
                            .tag(MetricsTags.TAG_KEY_TAG.getName(), Variant.ofString("environment"))
                            .tag(MetricsTags.TAG_VALUE_TAG.getName(), Variant.ofString("dev"))
                            .build(),
                    Container.builder()
                            .tag(MetricsTags.TAG_KEY_TAG.getName(), Variant.ofString("_name"))
                            .tag(MetricsTags.TAG_VALUE_TAG.getName(), Variant.ofString("metric.name"))
                            .build()
            )))
            .build();

    @Test
    public void shouldConvertMetricWithTagsSanitizingMetricName() {
        MetricConverter converter = new MetricWithTagsEventConverter(GraphiteSanitizer.METRIC_NAME_SANITIZER);
        assertMetricName("metric_name;project=hercules;subproject=graphite-sink;environment=dev", metric, converter);
    }

    @Test
    public void shouldConvertMetricWithoutTagsSanitizingMetricName() {
        MetricConverter converter = new MetricEventConverter(GraphiteSanitizer.METRIC_NAME_SANITIZER);
        assertMetricName("hercules.graphite-sink.dev.metric_name", metric, converter);
    }

    @Test
    public void shouldConvertMetricWithTagsPreservingMetricName() {
        MetricConverter converter = new MetricWithTagsEventConverter(GraphiteSanitizer.METRIC_PATH_SANITIZER);
        assertMetricName("metric.name;project=hercules;subproject=graphite-sink;environment=dev", metric, converter);
    }

    @Test
    public void shouldConvertMetricWithoutTagsPreservingMetricName() {
        MetricConverter converter = new MetricEventConverter(GraphiteSanitizer.METRIC_PATH_SANITIZER);
        assertMetricName("hercules.graphite-sink.dev.metric.name", metric, converter);
    }

    private static void assertMetricName(String expectedMetricName, Event metric, MetricConverter converter) {
        assertEquals(expectedMetricName, converter.convert(metric).getMetricName());
    }
}
