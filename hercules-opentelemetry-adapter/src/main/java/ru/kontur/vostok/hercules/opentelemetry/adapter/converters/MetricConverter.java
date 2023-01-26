package ru.kontur.vostok.hercules.opentelemetry.adapter.converters;

import io.opentelemetry.proto.common.v1.AnyValue;
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
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventBuilder;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.Vector;
import ru.kontur.vostok.hercules.tags.MetricsTags;
import ru.kontur.vostok.hercules.util.time.TimeUtil;
import ru.kontur.vostok.hercules.uuid.UuidGenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Metric converter is used to convert OpenTelemetry ResourceMetrics to Hercules events.
 *
 * @author Innokentiy Krivonosov
 * @see <a href="https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/metrics/data-model.md">
 * Metrics Data Model</a>
 * @see <a href="https://github.com/open-telemetry/opentelemetry-proto/blob/main/opentelemetry/proto/metrics/v1/metrics.proto">
 * metrics.proto</a>
 */
public class MetricConverter {
    public static List<Event> convert(ResourceMetrics resourceMetrics) {
        Resource resource = resourceMetrics.getResource();

        List<Container> resourceTags = getResourceTags(resource);

        return resourceMetrics
                .getScopeMetricsList()
                .stream()
                .flatMap(it -> convertMetrics(it, resourceTags))
                .collect(Collectors.toList());
    }

    private static Stream<Event> convertMetrics(ScopeMetrics scopeMetrics, List<Container> resourceTags) {
        String scopeName = scopeMetrics.getScope().getName();

        List<Container> resourceAndScopeTags = new ArrayList<>(resourceTags.size() + 1);
        resourceAndScopeTags.addAll(resourceTags);
        resourceAndScopeTags.add(getTagContainer(SCOPE_NAME_TAG_KEY, Variant.ofString(scopeName)));

        return scopeMetrics
                .getMetricsList()
                .stream()
                .flatMap(it -> convertMetric(it, resourceAndScopeTags).stream());
    }

    private static List<Event> convertMetric(Metric metric, List<Container> resourceAndScopeTags) {
        String metricName = metric.getName();

        switch (metric.getDataCase()) {
            case GAUGE:
                return convertGauge(metricName, metric.getGauge(), resourceAndScopeTags);
            case SUM:
                return convertSumMetric(metricName, metric.getSum(), resourceAndScopeTags);
            case HISTOGRAM:
                return convertHistogram(metricName, metric.getHistogram(), resourceAndScopeTags);
            case SUMMARY:
                return convertSummary(metricName, metric.getSummary(), resourceAndScopeTags);
            case EXPONENTIAL_HISTOGRAM:
            case DATA_NOT_SET:
                return Collections.emptyList();
        }

        return Collections.emptyList();
    }

    private static List<Event> convertGauge(String metricName, Gauge gauge, List<Container> resourceAndScopeTags) {
        List<Event> events = new ArrayList<>();
        for (NumberDataPoint numberDataPoint : gauge.getDataPointsList()) {
            addNumberDataPoint(metricName, events, numberDataPoint, resourceAndScopeTags);
        }

        return events;
    }

    private static List<Event> convertSumMetric(String metricName, Sum sum, List<Container> resourceAndScopeTags) {
        List<Event> events = new ArrayList<>();
        for (NumberDataPoint numberDataPoint : sum.getDataPointsList()) {
            addNumberDataPoint(metricName, events, numberDataPoint, resourceAndScopeTags);
        }

        return events;
    }

    private static List<Event> convertHistogram(String metricName, Histogram histogram, List<Container> resourceAndScopeTags) {
        List<Event> events = new ArrayList<>();
        for (HistogramDataPoint histogramDataPoint : histogram.getDataPointsList()) {
            addHistogramDataPoint(metricName, events, histogramDataPoint, resourceAndScopeTags);
        }

        return events;
    }

    private static List<Event> convertSummary(String metricName, Summary summary, List<Container> resourceAndScopeTags) {
        List<Event> events = new ArrayList<>();
        for (SummaryDataPoint summaryDataPoint : summary.getDataPointsList()) {
            addSummaryDataPoint(metricName, events, summaryDataPoint, resourceAndScopeTags);
        }

        return events;
    }

    private static void addNumberDataPoint(String metricName, List<Event> events, NumberDataPoint dataPoint, List<Container> resourceAndScopeTags) {
        double value;
        switch (dataPoint.getValueCase()) {
            case AS_INT:
                value = (double) dataPoint.getAsInt();
                break;
            case AS_DOUBLE:
                value = dataPoint.getAsDouble();
                break;
            default:
                return;
        }

        Container[] tags = getTags(metricName, dataPoint.getAttributesList(), resourceAndScopeTags);
        events.add(getEvent(dataPoint.getTimeUnixNano(), tags, value));
    }

    private static void addHistogramDataPoint(String metricName, List<Event> events, HistogramDataPoint dataPoint, List<Container> resourceAndScopeTags) {
        long timestamp = dataPoint.getTimeUnixNano();
        List<KeyValue> attributes = dataPoint.getAttributesList();

        for (int i = 0; i < Math.min(dataPoint.getExplicitBoundsCount(), dataPoint.getBucketCountsCount()); i++) {
            double explicitBound = dataPoint.getExplicitBounds(i);

            Container leTag = getTagContainer("le", getFormattedExplicitBounds(explicitBound));
            Container[] tags = getTags(metricName, attributes, resourceAndScopeTags, leTag);
            events.add(getEvent(timestamp, tags, dataPoint.getBucketCounts(i)));
        }

        if (!dataPoint.getBucketCountsList().isEmpty()) {
            Container infAddTag = getTagContainer("le", "inf");
            Container[] infTags = getTags(metricName, attributes, resourceAndScopeTags, infAddTag);
            double infValue = dataPoint.getBucketCounts(dataPoint.getBucketCountsCount() - 1);
            events.add(getEvent(timestamp, infTags, infValue));
        }

        if (dataPoint.hasMax()) {
            Container[] maxTags = getTags(metricName + MAX_SUFFIX, attributes, resourceAndScopeTags);
            events.add(getEvent(timestamp, maxTags, dataPoint.getMax()));
        }

        if (dataPoint.hasMin()) {
            Container[] minTags = getTags(metricName + MIN_SUFFIX, attributes, resourceAndScopeTags);
            events.add(getEvent(timestamp, minTags, dataPoint.getMin()));
        }

        if (dataPoint.hasSum()) {
            Container[] sumTags = getTags(metricName + SUM_SUFFIX, attributes, resourceAndScopeTags);
            events.add(getEvent(timestamp, sumTags, dataPoint.getSum()));
        }

        Container[] countTags = getTags(metricName + COUNT_SUFFIX, attributes, resourceAndScopeTags);
        events.add(getEvent(timestamp, countTags, dataPoint.getCount()));
    }

    private static void addSummaryDataPoint(String metricName, List<Event> events, SummaryDataPoint dataPoint, List<Container> resourceAndScopeTags) {
        long timestamp = dataPoint.getTimeUnixNano();
        List<KeyValue> attributes = dataPoint.getAttributesList();

        for (SummaryDataPoint.ValueAtQuantile valueAtQuantile : dataPoint.getQuantileValuesList()) {
            Container quantileTag = getTagContainer("quantile", getFormattedQuantile(valueAtQuantile.getQuantile()));
            Container[] tags = getTags(metricName, attributes, resourceAndScopeTags, quantileTag);
            events.add(getEvent(timestamp, tags, valueAtQuantile.getValue()));
        }

        Container[] sumTags = getTags(metricName + SUM_SUFFIX, attributes, resourceAndScopeTags);
        events.add(getEvent(timestamp, sumTags, dataPoint.getSum()));

        Container[] countTags = getTags(metricName + COUNT_SUFFIX, attributes, resourceAndScopeTags);
        events.add(getEvent(timestamp, countTags, dataPoint.getCount()));
    }

    static List<Container> getResourceTags(Resource resource) {
        Map<String, String> resourceTags = new LinkedHashMap<>();

        for (KeyValue keyValue : resource.getAttributesList()) {
            if (keyValue.getKey().startsWith(ResourceAttributes.METRIC_RESOURCE_PREFIX)) {
                String tagKey = keyValue.getKey().substring(ResourceAttributes.METRIC_RESOURCE_PREFIX.length());

                resourceTags.put(tagKey, convertToString(keyValue.getValue()));
            } else {
                String tagKey = RESOURCE_CONVERT_MAP.get(keyValue.getKey());

                if (tagKey != null && resource.getAttributesList().stream()
                        .noneMatch(it -> it.getKey().equals(ResourceAttributes.METRIC_RESOURCE_PREFIX + tagKey))
                ) {
                    resourceTags.put(tagKey, convertToString(keyValue.getValue()));
                }
            }
        }

        return resourceTags.entrySet().stream()
                .map(entry -> getTagContainer(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    private static Container[] getTags(String metricName, List<KeyValue> attributeTags, List<Container> resourceAndScopeTags) {
        return getTags(metricName, attributeTags, resourceAndScopeTags, null);
    }

    private static Container[] getTags(
            String metricName,
            List<KeyValue> attributeTags,
            List<Container> resourceAndScopeTags,
            Container addTag
    ) {
        Container[] tags = new Container[1 + resourceAndScopeTags.size() + 1 + attributeTags.size() + (addTag != null ? 1 : 0)];

        int iterator = 0;

        tags[iterator++] = getTagContainer(METRICS_DATA_TYPE_TAG_KEY, METRICS_DATA_TYPE_TAG_VALUE);

        for (Container tag : resourceAndScopeTags) {
            tags[iterator++] = tag;
        }

        tags[iterator++] = getTagContainer(METRIC_NAME_TAG_KEY, Variant.ofString(metricName));

        for (KeyValue tag : attributeTags) {
            tags[iterator++] = getTagContainer(tag.getKey(), convertToString(tag.getValue()));
        }

        if (addTag != null) {
            tags[iterator] = addTag;
        }

        return tags;
    }

    private static String getFormattedExplicitBounds(Double value) {
        if ((value == Math.floor(value)) && !Double.isInfinite(value)) {
            return value.longValue() + "";
        } else {
            return value + "";
        }
    }

    private static String getFormattedQuantile(Double value) {
        if ((value == Math.floor(value)) && !Double.isInfinite(value)) {
            return value.longValue() + "";
        } else {
            double withDecimal = value * 10;
            if ((withDecimal == Math.floor(withDecimal)) && !Double.isInfinite(value)) {
                return ((Double) withDecimal).longValue() + "";
            } else {
                return value + "";
            }
        }
    }

    private static String convertToString(AnyValue value) {
        switch (value.getValueCase()) {
            case STRING_VALUE:
                return value.getStringValue() + "";
            case BOOL_VALUE:
                return value.getBoolValue() + "";
            case INT_VALUE:
                return value.getIntValue() + "";
            case DOUBLE_VALUE:
                return value.getDoubleValue() + "";
            default:
                return "unknown";
        }
    }

    private static Container getTagContainer(String tagKey, String tagValue) {
        return getTagContainer(Variant.ofString(tagKey), Variant.ofString(tagValue));
    }

    private static Container getTagContainer(Variant tagKey, Variant tagValue) {
        return Container.builder()
                .tag(MetricsTags.TAG_KEY_TAG.getName(), tagKey)
                .tag(MetricsTags.TAG_VALUE_TAG.getName(), tagValue)
                .build();
    }

    private static Event getEvent(long timestamp, Container[] tags, double value) {
        return EventBuilder.create()
                .timestamp(TimeUtil.nanosToTicks(timestamp))
                .uuid(UuidGenerator.getClientInstance().next())
                .tag(MetricsTags.TAGS_VECTOR_TAG.getName(), Variant.ofVector(Vector.ofContainers(tags)))
                .tag(MetricsTags.METRIC_VALUE_TAG.getName(), Variant.ofDouble(value))
                .build();
    }

    private static final String MAX_SUFFIX = "_max";
    private static final String MIN_SUFFIX = "_min";
    private static final String SUM_SUFFIX = "_sum";
    private static final String COUNT_SUFFIX = "_count";

    private static final Variant SCOPE_NAME_TAG_KEY = Variant.ofString("scope");
    private static final Variant METRIC_NAME_TAG_KEY = Variant.ofString("_name");

    private static final Variant METRICS_DATA_TYPE_TAG_KEY = Variant.ofString("__metrics_data_type");
    private static final Variant METRICS_DATA_TYPE_TAG_VALUE = Variant.ofString("_tagged_metrics_");

    private static final Map<String, String> RESOURCE_CONVERT_MAP = Map.ofEntries(
            Map.entry(ResourceAttributes.SERVICE_NAME, VostokAnnotations.APPLICATION.toString()),
            Map.entry(ResourceAttributes.HOST_NAME, VostokAnnotations.HOST.toString()),
            Map.entry(ResourceAttributes.SERVICE_INSTANCE_ID, "instance")
    );
}
