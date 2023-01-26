package ru.kontur.vostok.hercules.opentelemetry.adapter.converters;

import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.InstrumentationScope;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.metrics.v1.Gauge;
import io.opentelemetry.proto.metrics.v1.Histogram;
import io.opentelemetry.proto.metrics.v1.HistogramDataPoint;
import io.opentelemetry.proto.metrics.v1.Metric;
import io.opentelemetry.proto.metrics.v1.NumberDataPoint;
import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import io.opentelemetry.proto.metrics.v1.ScopeMetrics;
import io.opentelemetry.proto.metrics.v1.Sum;
import io.opentelemetry.proto.metrics.v1.Summary;
import io.opentelemetry.proto.metrics.v1.SummaryDataPoint;
import io.opentelemetry.proto.resource.v1.Resource;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.protocol.util.TagDescription;
import ru.kontur.vostok.hercules.tags.MetricsTags;
import ru.kontur.vostok.hercules.util.time.TimeSource;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static ru.kontur.vostok.hercules.opentelemetry.adapter.converters.TraceConverterTest.getStringValueAttr;

/**
 * @author Innokentiy Krivonosov
 */
public class MetricConverterTest {

    @Test
    public void gauge() {
        long timestamp = TimeSource.SYSTEM.nanoseconds();
        NumberDataPoint numberDataPoint = NumberDataPoint.newBuilder()
                .addAttributes(KeyValue.newBuilder().setKey("code").setValue(AnyValue.newBuilder().setIntValue(200).build()).build())
                .addAttributes(getStringValueAttr("handler", "handlerName"))
                .setAsDouble(1.2)
                .setTimeUnixNano(timestamp)
                .build();

        Gauge gauge = Gauge.newBuilder()
                .addDataPoints(numberDataPoint)
                .build();

        Metric metric = Metric.newBuilder()
                .setName("metricName")
                .setGauge(gauge)
                .build();

        ResourceMetrics resourceMetrics = getResourceMetrics(metric);

        List<Event> events = MetricConverter.convert(resourceMetrics);
        assertEquals(1, events.size());
        assertEquals(1.2, extractValue(events.get(0)), 0.000000001);
        assertEquals(TimeUtil.nanosToTicks(timestamp), events.get(0).getTimestamp(), 0.000000001);
        Container[] tags = extractTags(events.get(0));

        assertEquals(10, tags.length);

        assertEquals("__metrics_data_type", extractTag(tags[0], MetricsTags.TAG_KEY_TAG));
        assertEquals("_tagged_metrics_", extractTag(tags[0], MetricsTags.TAG_VALUE_TAG));

        assertEquals("project", extractTag(tags[1], MetricsTags.TAG_KEY_TAG));
        assertEquals("metric-project", extractTag(tags[1], MetricsTags.TAG_VALUE_TAG));

        assertEquals("environment", extractTag(tags[2], MetricsTags.TAG_KEY_TAG));
        assertEquals("metric-env", extractTag(tags[2], MetricsTags.TAG_VALUE_TAG));

        assertEquals("application", extractTag(tags[3], MetricsTags.TAG_KEY_TAG));
        assertEquals("test-app", extractTag(tags[3], MetricsTags.TAG_VALUE_TAG));

        assertEquals("host", extractTag(tags[4], MetricsTags.TAG_KEY_TAG));
        assertEquals("test-s1", extractTag(tags[4], MetricsTags.TAG_VALUE_TAG));

        assertEquals("instance", extractTag(tags[5], MetricsTags.TAG_KEY_TAG));
        assertEquals("instance-id-1", extractTag(tags[5], MetricsTags.TAG_VALUE_TAG));

        assertEquals("scope", extractTag(tags[6], MetricsTags.TAG_KEY_TAG));
        assertEquals("scopeName", extractTag(tags[6], MetricsTags.TAG_VALUE_TAG));

        assertEquals("_name", extractTag(tags[7], MetricsTags.TAG_KEY_TAG));
        assertEquals("metricName", extractTag(tags[7], MetricsTags.TAG_VALUE_TAG));

        assertEquals("code", extractTag(tags[8], MetricsTags.TAG_KEY_TAG));
        assertEquals("200", extractTag(tags[8], MetricsTags.TAG_VALUE_TAG));

        assertEquals("handler", extractTag(tags[9], MetricsTags.TAG_KEY_TAG));
        assertEquals("handlerName", extractTag(tags[9], MetricsTags.TAG_VALUE_TAG));
    }

    @Test
    public void sum() {
        long timestamp = TimeSource.SYSTEM.nanoseconds();
        NumberDataPoint numberDataPoint = NumberDataPoint.newBuilder()
                .addAttributes(KeyValue.newBuilder().setKey("code").setValue(AnyValue.newBuilder().setIntValue(200).build()).build())
                .addAttributes(getStringValueAttr("handler", "handlerName"))
                .setAsInt(12)
                .setTimeUnixNano(timestamp)
                .build();

        Sum sum = Sum.newBuilder()
                .addDataPoints(numberDataPoint)
                .build();

        Metric metric = Metric.newBuilder()
                .setName("metricName")
                .setSum(sum)
                .build();

        ResourceMetrics resourceMetrics = getResourceMetrics(metric);

        List<Event> events = MetricConverter.convert(resourceMetrics);
        assertEquals(1, events.size());
        assertEquals(12, extractValue(events.get(0)), 0.000000001);
        Container[] tags = extractTags(events.get(0));
        assertEquals("_name", extractTag(tags[7], MetricsTags.TAG_KEY_TAG));
        assertEquals("metricName", extractTag(tags[7], MetricsTags.TAG_VALUE_TAG));
    }

    @Test
    public void histogram() {
        long timestamp = TimeSource.SYSTEM.nanoseconds();
        HistogramDataPoint numberDataPoint = HistogramDataPoint.newBuilder()
                .addAttributes(KeyValue.newBuilder().setKey("code").setValue(AnyValue.newBuilder().setIntValue(200).build()).build())
                .addAttributes(getStringValueAttr("handler", "handlerName"))
                .setSum(1000.0)
                .setCount(10)
                .setMax(20.0)
                .setMin(0.1)
                .addAllBucketCounts(List.of(0L, 10L, 40L, 3L, 2L, 1L))
                .addAllExplicitBounds(List.of(0d, 5d, 10d, 100d, 1000d))
                .setTimeUnixNano(timestamp)
                .build();

        Histogram histogram = Histogram.newBuilder()
                .addDataPoints(numberDataPoint)
                .build();

        Metric metric = Metric.newBuilder()
                .setName("metricName")
                .setHistogram(histogram)
                .build();

        ResourceMetrics resourceMetrics = getResourceMetrics(metric);

        List<Event> events = MetricConverter.convert(resourceMetrics);
        assertEquals(10, events.size());

        {
            assertEquals(0, extractValue(events.get(0)), 0.000000001);
            Container[] tags = extractTags(events.get(0));
            assertEquals(11, tags.length);
            assertEquals("_name", extractTag(tags[7], MetricsTags.TAG_KEY_TAG));
            assertEquals("metricName", extractTag(tags[7], MetricsTags.TAG_VALUE_TAG));
            assertEquals("le", extractTag(tags[10], MetricsTags.TAG_KEY_TAG));
            assertEquals("0", extractTag(tags[10], MetricsTags.TAG_VALUE_TAG));
        }
        {
            assertEquals(10, extractValue(events.get(1)), 0.000000001);
            Container[] tags = extractTags(events.get(1));
            assertEquals("_name", extractTag(tags[7], MetricsTags.TAG_KEY_TAG));
            assertEquals("metricName", extractTag(tags[7], MetricsTags.TAG_VALUE_TAG));
            assertEquals("le", extractTag(tags[10], MetricsTags.TAG_KEY_TAG));
            assertEquals("5", extractTag(tags[10], MetricsTags.TAG_VALUE_TAG));
        }
        {
            assertEquals(40, extractValue(events.get(2)), 0.000000001);
            Container[] tags = extractTags(events.get(2));
            assertEquals("_name", extractTag(tags[7], MetricsTags.TAG_KEY_TAG));
            assertEquals("metricName", extractTag(tags[7], MetricsTags.TAG_VALUE_TAG));
            assertEquals("le", extractTag(tags[10], MetricsTags.TAG_KEY_TAG));
            assertEquals("10", extractTag(tags[10], MetricsTags.TAG_VALUE_TAG));
        }
        {
            assertEquals(3, extractValue(events.get(3)), 0.000000001);
            Container[] tags = extractTags(events.get(3));
            assertEquals("_name", extractTag(tags[7], MetricsTags.TAG_KEY_TAG));
            assertEquals("metricName", extractTag(tags[7], MetricsTags.TAG_VALUE_TAG));
            assertEquals("le", extractTag(tags[10], MetricsTags.TAG_KEY_TAG));
            assertEquals("100", extractTag(tags[10], MetricsTags.TAG_VALUE_TAG));
        }
        {
            assertEquals(2, extractValue(events.get(4)), 0.000000001);
            Container[] tags = extractTags(events.get(4));
            assertEquals("_name", extractTag(tags[7], MetricsTags.TAG_KEY_TAG));
            assertEquals("metricName", extractTag(tags[7], MetricsTags.TAG_VALUE_TAG));
            assertEquals("le", extractTag(tags[10], MetricsTags.TAG_KEY_TAG));
            assertEquals("1000", extractTag(tags[10], MetricsTags.TAG_VALUE_TAG));
        }
        {
            assertEquals(1, extractValue(events.get(5)), 0.000000001);
            Container[] tags = extractTags(events.get(5));
            assertEquals("_name", extractTag(tags[7], MetricsTags.TAG_KEY_TAG));
            assertEquals("metricName", extractTag(tags[7], MetricsTags.TAG_VALUE_TAG));
            assertEquals("le", extractTag(tags[10], MetricsTags.TAG_KEY_TAG));
            assertEquals("inf", extractTag(tags[10], MetricsTags.TAG_VALUE_TAG));
        }

        {
            assertEquals(20, extractValue(events.get(6)), 0.000000001);
            Container[] tags = extractTags(events.get(6));
            assertEquals(10, tags.length);
            assertEquals("_name", extractTag(tags[7], MetricsTags.TAG_KEY_TAG));
            assertEquals("metricName_max", extractTag(tags[7], MetricsTags.TAG_VALUE_TAG));
        }
        {
            assertEquals(0.1, extractValue(events.get(7)), 0.000000001);
            Container[] tags = extractTags(events.get(7));
            assertEquals(10, tags.length);
            assertEquals("_name", extractTag(tags[7], MetricsTags.TAG_KEY_TAG));
            assertEquals("metricName_min", extractTag(tags[7], MetricsTags.TAG_VALUE_TAG));
        }
        {
            assertEquals(1000.0, extractValue(events.get(8)), 0.000000001);
            Container[] tags = extractTags(events.get(8));
            assertEquals(10, tags.length);
            assertEquals("_name", extractTag(tags[7], MetricsTags.TAG_KEY_TAG));
            assertEquals("metricName_sum", extractTag(tags[7], MetricsTags.TAG_VALUE_TAG));
        }
        {
            assertEquals(10, extractValue(events.get(9)), 0.000000001);
            Container[] tags = extractTags(events.get(9));
            assertEquals(10, tags.length);
            assertEquals("_name", extractTag(tags[7], MetricsTags.TAG_KEY_TAG));
            assertEquals("metricName_count", extractTag(tags[7], MetricsTags.TAG_VALUE_TAG));
        }
    }

    @Test
    public void summary() {
        long timestamp = TimeSource.SYSTEM.nanoseconds();
        SummaryDataPoint numberDataPoint = SummaryDataPoint.newBuilder()
                .addAttributes(KeyValue.newBuilder().setKey("code").setValue(AnyValue.newBuilder().setIntValue(200).build()).build())
                .addAttributes(getStringValueAttr("handler", "handlerName"))
                .setSum(10000.0)
                .setCount(10)
                .addQuantileValues(SummaryDataPoint.ValueAtQuantile.newBuilder().setQuantile(50.0).setValue(20.0).build())
                .addQuantileValues(SummaryDataPoint.ValueAtQuantile.newBuilder().setQuantile(99.9).setValue(800.0).build())
                .setTimeUnixNano(timestamp)
                .build();

        Summary summary = Summary.newBuilder()
                .addDataPoints(numberDataPoint)
                .build();

        Metric metric = Metric.newBuilder()
                .setName("metricName")
                .setSummary(summary)
                .build();

        ResourceMetrics resourceMetrics = getResourceMetrics(metric);

        List<Event> events = MetricConverter.convert(resourceMetrics);
        assertEquals(4, events.size());

        {
            assertEquals(20.0, extractValue(events.get(0)), 0.000000001);
            Container[] tags = extractTags(events.get(0));
            assertEquals(11, tags.length);
            assertEquals("_name", extractTag(tags[7], MetricsTags.TAG_KEY_TAG));
            assertEquals("metricName", extractTag(tags[7], MetricsTags.TAG_VALUE_TAG));
            assertEquals("quantile", extractTag(tags[10], MetricsTags.TAG_KEY_TAG));
            assertEquals("50", extractTag(tags[10], MetricsTags.TAG_VALUE_TAG));
        }
        {
            assertEquals(800.0, extractValue(events.get(1)), 0.000000001);
            Container[] tags = extractTags(events.get(1));
            assertEquals("_name", extractTag(tags[7], MetricsTags.TAG_KEY_TAG));
            assertEquals("metricName", extractTag(tags[7], MetricsTags.TAG_VALUE_TAG));
            assertEquals("quantile", extractTag(tags[10], MetricsTags.TAG_KEY_TAG));
            assertEquals("999", extractTag(tags[10], MetricsTags.TAG_VALUE_TAG));
        }

        {
            assertEquals(10000.0, extractValue(events.get(2)), 0.000000001);
            Container[] tags = extractTags(events.get(2));
            assertEquals(10, tags.length);
            assertEquals("_name", extractTag(tags[7], MetricsTags.TAG_KEY_TAG));
            assertEquals("metricName_sum", extractTag(tags[7], MetricsTags.TAG_VALUE_TAG));
        }
        {
            assertEquals(10, extractValue(events.get(3)), 0.000000001);
            Container[] tags = extractTags(events.get(3));
            assertEquals(10, tags.length);
            assertEquals("_name", extractTag(tags[7], MetricsTags.TAG_KEY_TAG));
            assertEquals("metricName_count", extractTag(tags[7], MetricsTags.TAG_VALUE_TAG));
        }
    }

    @Test
    public void resourceTagsOrder() {
        Resource resource = getResource();
        List<Container> resourceTags = MetricConverter.getResourceTags(resource);
        assertEquals(5, resourceTags.size());
        assertEquals("project", extractTag(resourceTags.get(0), MetricsTags.TAG_KEY_TAG));
        assertEquals("environment", extractTag(resourceTags.get(1), MetricsTags.TAG_KEY_TAG));
        assertEquals("application", extractTag(resourceTags.get(2), MetricsTags.TAG_KEY_TAG));
        assertEquals("host", extractTag(resourceTags.get(3), MetricsTags.TAG_KEY_TAG));
        assertEquals("instance", extractTag(resourceTags.get(4), MetricsTags.TAG_KEY_TAG));
    }

    @Test
    public void resourceTagsOrderAttrWithMetricPrefixLater() {
        Resource resource = Resource.newBuilder()
                .addAttributes(getStringValueAttr(ResourceAttributes.METRIC_RESOURCE_PREFIX + "project", "metric-project"))
                .addAttributes(getStringValueAttr(ResourceAttributes.SERVICE_NAME, "test-app"))
                .addAttributes(getStringValueAttr("environment", "trace-env"))
                .addAttributes(getStringValueAttr(ResourceAttributes.METRIC_RESOURCE_PREFIX + "environment", "metric-env"))
                .addAttributes(getStringValueAttr(ResourceAttributes.HOST_NAME, "test-s1"))
                .addAttributes(getStringValueAttr(ResourceAttributes.SERVICE_INSTANCE_ID, "instance-id-1"))
                .build();
        List<Container> resourceTags = MetricConverter.getResourceTags(resource);
        assertEquals(5, resourceTags.size());
        assertEquals("project", extractTag(resourceTags.get(0), MetricsTags.TAG_KEY_TAG));
        assertEquals("application", extractTag(resourceTags.get(1), MetricsTags.TAG_KEY_TAG));
        assertEquals("environment", extractTag(resourceTags.get(2), MetricsTags.TAG_KEY_TAG));
        assertEquals("host", extractTag(resourceTags.get(3), MetricsTags.TAG_KEY_TAG));
        assertEquals("instance", extractTag(resourceTags.get(4), MetricsTags.TAG_KEY_TAG));
    }

    @NotNull
    private static Double extractValue(Event event) {
        return ContainerUtil.extract(event.getPayload(), MetricsTags.METRIC_VALUE_TAG).get();
    }

    @NotNull
    private static Container[] extractTags(Event event) {
        return ContainerUtil.extract(event.getPayload(), MetricsTags.TAGS_VECTOR_TAG).get();
    }

    @NotNull
    private static String extractTag(Container container, TagDescription<Optional<String>> tagKeyTag) {
        return ContainerUtil.extract(container, tagKeyTag).get();
    }

    @NotNull
    private static ResourceMetrics getResourceMetrics(Metric metric) {
        Resource resource = getResource();

        ScopeMetrics scopeMetrics = ScopeMetrics.newBuilder()
                .setScope(InstrumentationScope.newBuilder().setName("scopeName").build())
                .addMetrics(metric).build();

        return ResourceMetrics.newBuilder()
                .setResource(resource)
                .addScopeMetrics(scopeMetrics)
                .build();
    }

    @NotNull
    private static Resource getResource() {
        return Resource.newBuilder()
                .addAttributes(getStringValueAttr(ResourceAttributes.METRIC_RESOURCE_PREFIX + "project", "metric-project"))
                .addAttributes(getStringValueAttr(ResourceAttributes.METRIC_RESOURCE_PREFIX + "environment", "metric-env"))
                .addAttributes(getStringValueAttr(ResourceAttributes.SERVICE_NAME, "test-app"))
                .addAttributes(getStringValueAttr("environment", "trace-env"))
                .addAttributes(getStringValueAttr(ResourceAttributes.HOST_NAME, "test-s1"))
                .addAttributes(getStringValueAttr(ResourceAttributes.SERVICE_INSTANCE_ID, "instance-id-1"))
                .build();
    }

}