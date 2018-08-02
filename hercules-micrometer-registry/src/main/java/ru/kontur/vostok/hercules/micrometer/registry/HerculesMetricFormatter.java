package ru.kontur.vostok.hercules.micrometer.registry;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Counting;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metered;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricAttribute;
import com.codahale.metrics.Sampling;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import java.util.Set;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.encoder.EventBuilder;
import ru.kontur.vostok.hercules.uuid.UuidGenerator;

/**
 * @author Daniil Zhenikhov
 */
public final class HerculesMetricFormatter {
    private static final UuidGenerator GENERATOR = UuidGenerator.getClientInstance();

    private final String rateUnit;
    private final String durationUnit;
    private final Set<MetricAttribute> disabledMetricAttributes;

    public HerculesMetricFormatter(String rateUnit,
                                   String durationUnit,
                                   Set<MetricAttribute> disableMetricAttributes) {
        this.rateUnit = rateUnit;
        this.durationUnit = durationUnit;
        this.disabledMetricAttributes = disableMetricAttributes;
    }

    public Event formatGauge(String name, Gauge metric, long timestamp) {
        return formatMetric(name, metric, timestamp, this::consumeGauge);
    }

    public Event formatCounter(String name, Counter metric, long timestamp) {
        return formatMetric(name, metric, timestamp, this::consumeCounter);
    }

    public Event formatHistogram(String name, Histogram metric, long timestamp) {
        return formatMetric(name, metric, timestamp, this::consumeHistogram);
    }

    public Event formatMeter(String name, Meter metric, long timestamp) {
        return formatMetric(name, metric, timestamp, this::consumeMeter);
    }

    public Event formatTimer(String name, Timer metric, long timestamp) {
        return formatMetric(name, metric, timestamp, this::consumeTimer);
    }

    /**
     * Create base for event. Add tag "name", tag "timestamp", setting version, setting event id.
     * After do consuming of specified metric type.
     */
    private <T extends Metric> Event formatMetric(String name,
                                                  T metric,
                                                  long timestamp,
                                                  MetricConsumer<T> consumer) {
        EventBuilder eventBuilder = new EventBuilder();
        eventBuilder.setVersion(1);
        eventBuilder.setEventId(GENERATOR.next());

        eventBuilder.setTag("timestamp", Variant.ofLong(timestamp));
        eventBuilder.setTag("name", Variant.ofString(name));
        consumer.consume(metric, eventBuilder);

        return eventBuilder.build();
    }

    /**
     * Consume {@link Gauge gauge} type metric. Setting
     * tag "value" - metric's current value
     */
    //TODO: add any gauge generic type value
    private void consumeGauge(Gauge gauge,
                              EventBuilder eventBuilder) {
        eventBuilder.setTag("value", Variant.ofString(String.valueOf(gauge.getValue())));
    }

    /**
     * Consume {@link Counter counter} type metric. Setting
     * {@code consumeCounting};
     */
    private void consumeCounter(Counter counter,
                                EventBuilder eventBuilder) {
        consumeCounting(counter, eventBuilder);
    }

    /**
     * Consume {@link Histogram histogram} type metric. Setting
     * {@code consumeCounting};
     * {@code consumeSampling};
     */
    private void consumeHistogram(Histogram histogram,
                                  EventBuilder eventBuilder) {
        consumeCounting(histogram, eventBuilder);
        consumeSampling(histogram, eventBuilder);
    }

    /**
     * Consume {@link Meter meter} type metric. Setting
     * {@code consumeMetered};
     * tag "duration_unit".
     */
    private void consumeMeter(Meter meter,
                              EventBuilder eventBuilder) {
        consumeMetered(meter, eventBuilder);
        eventBuilder.setTag("rate_unit", Variant.ofString(rateUnit));
    }

    /**
     * Consume {@link Timer timer} type metric. Setting
     * {@code consumeMetered};
     * {@code consumeSampling};
     * tag "rate_unit";
     * tag "duration_unit".
     */
    private void consumeTimer(Timer timer,
                              EventBuilder eventBuilder) {
        consumeMetered(timer, eventBuilder);
        consumeSampling(timer, eventBuilder);
        eventBuilder.setTag("rate_unit", Variant.ofString(rateUnit));
        eventBuilder.setTag("duration_unit", Variant.ofString(durationUnit));
    }

    /**
     * Consume {@link Sampling sampling} type metric. Setting
     * tag "max" - highest value in the snapshot;
     * tag "mean" - arithmetic mean of the values in the snapshot;
     * tag "min" - lowest value in the snapshot;
     * tag "stddev" - standard deviation of the values in the snapshot;
     * tag "p50" - median value in the distribution;
     * tag "p75" - value at the 75th percentile in the distribution;
     * tag "p95" - value at the 95 th percentile in the distribution;
     * tag "p98" - value at the 98th percentile in the distribution;
     * tag "p99" - value at the 99th percentile in the distribution;
     * tag "p999" - value at the 99.9th percentile in the distribution;
     */
    private void consumeSampling(Sampling sampling, EventBuilder eventBuilder) {
        final Snapshot snapshot = sampling.getSnapshot();

        setTagIfEnabled(eventBuilder, MetricAttribute.MAX, Variant.ofLong(snapshot.getMax()));
        setTagIfEnabled(eventBuilder, MetricAttribute.MEAN, Variant.ofDouble(snapshot.getMean()));
        setTagIfEnabled(eventBuilder, MetricAttribute.MIN, Variant.ofLong(snapshot.getMin()));
        setTagIfEnabled(eventBuilder, MetricAttribute.STDDEV, Variant.ofDouble(snapshot.getStdDev()));
        setTagIfEnabled(eventBuilder, MetricAttribute.P50, Variant.ofDouble(snapshot.getMedian()));
        setTagIfEnabled(eventBuilder, MetricAttribute.P75, Variant.ofDouble(snapshot.get75thPercentile()));
        setTagIfEnabled(eventBuilder, MetricAttribute.P95, Variant.ofDouble(snapshot.get95thPercentile()));
        setTagIfEnabled(eventBuilder, MetricAttribute.P98, Variant.ofDouble(snapshot.get98thPercentile()));
        setTagIfEnabled(eventBuilder, MetricAttribute.P99, Variant.ofDouble(snapshot.get99thPercentile()));
        setTagIfEnabled(eventBuilder, MetricAttribute.P999, Variant.ofDouble(snapshot.get999thPercentile()));
    }

    /**
     * Consume {@link Counting counting} metric type. Setting tag "count".
     */
    private void consumeCounting(Counting counting, EventBuilder eventBuilder) {
        setTagIfEnabled(eventBuilder, MetricAttribute.COUNT, Variant.ofLong(counting.getCount()));
    }

    /**
     * Consume {@link Metered metered} type metric. Setting
     * tag "mean_rate" - mean rate;
     * tag "m1_rate" - one-minute rate;
     * tag "m5_rate" - five-minutes rate;
     * tag "m15_rate" - fifteen-minutes rate;
     * Also do {@code consumeCounting}.
     */
    private void consumeMetered(Metered metered, EventBuilder eventBuilder) {
        consumeCounting(metered, eventBuilder);
        setTagIfEnabled(eventBuilder, MetricAttribute.MEAN_RATE, Variant.ofDouble(metered.getMeanRate()));
        setTagIfEnabled(eventBuilder, MetricAttribute.M1_RATE, Variant.ofDouble(metered.getOneMinuteRate()));
        setTagIfEnabled(eventBuilder, MetricAttribute.M5_RATE, Variant.ofDouble(metered.getFiveMinuteRate()));
        setTagIfEnabled(eventBuilder, MetricAttribute.M15_RATE, Variant.ofDouble(metered.getFifteenMinuteRate()));
    }

    private void setTagIfEnabled(EventBuilder eventBuilder, MetricAttribute metricAttribute, Variant variant) {
        if (disabledMetricAttributes.contains(metricAttribute)) {
            return;
        }
        eventBuilder.setTag(metricAttribute.getCode(), variant);
    }

    @FunctionalInterface
    private interface MetricConsumer<T extends Metric> {
        void consume(T metric, EventBuilder eventBuilder);
    }
}
