package ru.kontur.vostok.hercules.graphite.adapter.purgatory;

import ru.kontur.vostok.hercules.graphite.adapter.filter.PlainMetricAclFilter;
import ru.kontur.vostok.hercules.graphite.adapter.metric.Metric;
import ru.kontur.vostok.hercules.graphite.adapter.metric.MetricTag;
import ru.kontur.vostok.hercules.health.Meter;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.health.MetricsUtil;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventBuilder;
import ru.kontur.vostok.hercules.protocol.TinyString;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.Vector;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.text.AsciiUtil;
import ru.kontur.vostok.hercules.util.time.TimeUtil;
import ru.kontur.vostok.hercules.uuid.UuidGenerator;

import java.util.Properties;

/**
 * The purgatory processes metric events:
 * <ul>
 *   <li>Filters out invalid metrics.</li>
 *   <li>Filters out denied metrics.</li>
 *   <li>Builds {@link Event Hercules events}.</li>
 *   <li>Sends to the Gate using {@link Accumulator}</li>
 * </ul>
 * <p>
 * Metrics with tags and without tags are sent to different streams.
 *
 * @author Gregory Koshelev
 */
public class Purgatory {
    private static final TinyString TAGS_TAG = TinyString.of("tags");
    private static final TinyString KEY_TAG = TinyString.of("key");
    private static final TinyString VALUE_TAG = TinyString.of("value");
    private static final Variant NAME_KEY_TAG_VALUE = Variant.ofString("_name");

    private static final String METRICS_SCOPE = Purgatory.class.getSimpleName();

    private final UuidGenerator uuidGenerator = UuidGenerator.getClientInstance();

    private final Accumulator accumulator;

    private final PlainMetricAclFilter acl;

    private final String plainMetricsStream;
    private final String taggedMetricsStream;

    private final Meter invalidMetricsMeter;
    private final Meter deniedMetricsMeter;
    private final Meter plainMetricsMeter;
    private final Meter taggedMetricsMeter;

    public Purgatory(Properties properties, Accumulator accumulator, MetricsCollector metricsCollector) {
        this.accumulator = accumulator;

        this.acl = new PlainMetricAclFilter(PropertiesUtil.ofScope(properties, "filter.acl"));

        this.plainMetricsStream = PropertiesUtil.get(Props.PLAIN_METRICS_STREAM, properties).get();
        this.taggedMetricsStream = PropertiesUtil.get(Props.TAGGED_METRICS_STREAM, properties).get();

        this.invalidMetricsMeter = metricsCollector.meter(MetricsUtil.toMetricPathWithPrefix(METRICS_SCOPE, "invalidMetrics"));
        this.deniedMetricsMeter = metricsCollector.meter(MetricsUtil.toMetricPathWithPrefix(METRICS_SCOPE, "deniedMetrics"));
        this.plainMetricsMeter = metricsCollector.meter(MetricsUtil.toMetricPathWithPrefix(METRICS_SCOPE, "plainMetrics"));
        this.taggedMetricsMeter = metricsCollector.meter(MetricsUtil.toMetricPathWithPrefix(METRICS_SCOPE, "taggedMetrics"));
    }

    public void process(Metric metric) {
        if (!validate(metric)) {
            invalidMetricsMeter.mark();
            return;
        }

        if (!acl.test(metric)) {
            deniedMetricsMeter.mark();
            return;
        }

        if (metric.hasTags()) {
            taggedMetricsMeter.mark();

            MetricTag[] tags = metric.tags();
            Container[] tagContainers = new Container[tags.length + 1];
            for (int i = 0; i < tags.length; i++) {
                MetricTag tag = tags[i];
                tagContainers[i] = Container.builder().
                        tag(KEY_TAG, Variant.ofString(tag.key())).
                        tag(VALUE_TAG, Variant.ofString(tag.value())).
                        build();
            }
            tagContainers[tags.length] = Container.builder().
                    tag(KEY_TAG, NAME_KEY_TAG_VALUE).
                    tag(VALUE_TAG, Variant.ofString(metric.name())).
                    build();

            Event event = EventBuilder.create(TimeUtil.unixTimeToUnixTicks(metric.timestamp()), uuidGenerator.next()).
                    tag(TAGS_TAG, Variant.ofVector(Vector.ofContainers(tagContainers))).
                    tag(VALUE_TAG, Variant.ofDouble(metric.value())).
                    build();
            accumulator.add(taggedMetricsStream, event);
        } else {
            plainMetricsMeter.mark();

            Container nameContainer = Container.builder().
                    tag(KEY_TAG, NAME_KEY_TAG_VALUE).
                    tag(VALUE_TAG, Variant.ofString(metric.name())).
                    build();

            Event event = EventBuilder.create(TimeUtil.unixTimeToUnixTicks(metric.timestamp()), uuidGenerator.next()).
                    tag(TAGS_TAG, Variant.ofVector(Vector.ofContainers(nameContainer))).
                    tag(VALUE_TAG, Variant.ofDouble(metric.value())).
                    build();
            accumulator.add(plainMetricsStream, event);
        }
    }

    private boolean validate(Metric metric) {
        if (Double.isNaN(metric.value())) {
            return false;
        }

        byte[] metricName = metric.name();
        if (metricName.length == 0) {
            return false;
        }
        for (byte c : metricName) {
            if (!AsciiUtil.isAlphaNumeric(c) && !AsciiUtil.isDot(c) && !AsciiUtil.isUnderscore(c)) {
                return false;
            }
        }

        return true;
    }


    private static class Props {
        static Parameter<String> PLAIN_METRICS_STREAM =
                Parameter.stringParameter("plain.metrics.stream").
                        required().
                        build();

        static Parameter<String> TAGGED_METRICS_STREAM =
                Parameter.stringParameter("tagged.metrics.stream").
                        required().
                        build();
    }
}
