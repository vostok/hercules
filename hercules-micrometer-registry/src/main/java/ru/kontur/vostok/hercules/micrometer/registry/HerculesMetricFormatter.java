package ru.kontur.vostok.hercules.micrometer.registry;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Counting;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metered;
import com.codahale.metrics.Metric;
import com.codahale.metrics.Sampling;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import java.util.HashMap;
import java.util.Map;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.encoder.EventBuilder;
import ru.kontur.vostok.hercules.uuid.UuidGenerator;

/**
 * @author Daniil Zhenikhov
 */
public final class HerculesMetricFormatter {
    private static final Map<Class<? extends Metric>, MetricContainerCreator> MAPPER;
    private static final UuidGenerator GENERATOR = UuidGenerator.getClientInstance();

    static {
        MAPPER = new HashMap<>();

        MAPPER.put(Gauge.class, (name, metric, timestamp, rateUnit, durationUnit) ->
                formatMetric(name,
                        (Gauge) metric,
                        timestamp,
                        rateUnit,
                        durationUnit,
                        HerculesMetricFormatter::consumeGauge));
        MAPPER.put(Counter.class, (name, metric, timestamp, rateUnit, durationUnit) ->
                formatMetric(name,
                        (Counter) metric,
                        timestamp,
                        rateUnit,
                        durationUnit,
                        HerculesMetricFormatter::consumeCounter));
        MAPPER.put(Histogram.class, (name, metric, timestamp, rateUnit, durationUnit) ->
                formatMetric(name,
                        (Histogram) metric,
                        timestamp,
                        rateUnit,
                        durationUnit,
                        HerculesMetricFormatter::consumeHistogram));
        MAPPER.put(Meter.class, (name, metric, timestamp, rateUnit, durationUnit) ->
                formatMetric(name,
                        (Meter) metric,
                        timestamp,
                        rateUnit,
                        durationUnit,
                        HerculesMetricFormatter::consumeMeter));
        MAPPER.put(Timer.class, (name, metric, timestamp, rateUnit, durationUnit) ->
                formatMetric(name,
                        (Timer) metric,
                        timestamp,
                        rateUnit,
                        durationUnit,
                        HerculesMetricFormatter::consumeTimer));

    }

    private HerculesMetricFormatter() {

    }

    @SuppressWarnings("unchecked cast")
    public static <T extends Metric> Event createMetricEvent(String name,
                                                             T metric,
                                                             long timestamp,
                                                             String rateUnit,
                                                             String durationUnit) {
        if (!MAPPER.containsKey(metric.getClass())) {
            throw new IllegalStateException("Unknown metric class");
        }

        return MAPPER.get(metric.getClass())
                .create(name, metric, timestamp, rateUnit, durationUnit);
    }

    /**
     * Create base for event. Add tag "name", tag "timestamp", setting version, setting event id.
     * After do consuming of specified metric type.
     */
    private static <T extends Metric> Event formatMetric(String name,
                                                         T metric,
                                                         long timestamp,
                                                         String rateUnit,
                                                         String durationUnit,
                                                         MetricConsumer<T> consumer) {
        EventBuilder eventBuilder = new EventBuilder();
        eventBuilder.setVersion(1);
        eventBuilder.setEventId(GENERATOR.next());

        eventBuilder.setTag("timestamp", Variant.ofLong(timestamp));
        eventBuilder.setTag("name", Variant.ofString(name));
        consumer.consume(metric, rateUnit, durationUnit, eventBuilder);

        return eventBuilder.build();
    }

    /**
     * Consume {@link Gauge gauge} type metric. Setting
     * tag "value" - metric's current value
     */
    private static void consumeGauge(Gauge gauge,
                                     String rateUnit,
                                     String durationUnit,
                                     EventBuilder eventBuilder) {
        eventBuilder.setTag("value", Variant.ofString(String.valueOf(gauge.getValue())));
    }

    /**
     * Consume {@link Counter counter} type metric. Setting
     * {@code consumeCounting};
     */
    private static void consumeCounter(Counter counter,
                                       String rateUnit,
                                       String durationUnit,
                                       EventBuilder eventBuilder) {
        consumeCounting(counter, eventBuilder);
    }

    /**
     * Consume {@link Histogram histogram} type metric. Setting
     * {@code consumeCounting};
     * {@code consumeSampling};
     */
    private static void consumeHistogram(Histogram histogram,
                                         String rateUnit,
                                         String durationUnit,
                                         EventBuilder eventBuilder) {
        consumeCounting(histogram, eventBuilder);
        consumeSampling(histogram, eventBuilder);
    }

    /**
     * Consume {@link Meter meter} type metric. Setting
     * {@code consumeMetered};
     * tag "duration_unit".
     */
    private static void consumeMeter(Meter meter,
                                     String rateUnit,
                                     String durationUnit,
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
    private static void consumeTimer(Timer timer,
                                     String rateUnit,
                                     String durationUnit,
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
    private static void consumeSampling(Sampling sampling, EventBuilder eventBuilder) {
        final Snapshot snapshot = sampling.getSnapshot();

        eventBuilder.setTag("max", Variant.ofLong(snapshot.getMax()));
        eventBuilder.setTag("mean", Variant.ofDouble(snapshot.getMean()));
        eventBuilder.setTag("min", Variant.ofLong(snapshot.getMin()));
        eventBuilder.setTag("stddev", Variant.ofDouble(snapshot.getStdDev()));
        eventBuilder.setTag("p50", Variant.ofDouble(snapshot.getMedian()));
        eventBuilder.setTag("p75", Variant.ofDouble(snapshot.get75thPercentile()));
        eventBuilder.setTag("p95", Variant.ofDouble(snapshot.get95thPercentile()));
        eventBuilder.setTag("p98", Variant.ofDouble(snapshot.get98thPercentile()));
        eventBuilder.setTag("p99", Variant.ofDouble(snapshot.get99thPercentile()));
        eventBuilder.setTag("p999", Variant.ofDouble(snapshot.get999thPercentile()));
    }

    /**
     * Consume {@link Counting counting} metric type. Setting tag "count".
     */
    private static void consumeCounting(Counting counting, EventBuilder eventBuilder) {
        eventBuilder.setTag("count", Variant.ofLong(counting.getCount()));
    }

    /**
     * Consume {@link Metered metered} type metric. Setting
     * tag "mean_rate" - mean rate;
     * tag "m1_rate" - one-minute rate;
     * tag "m5_rate" - five-minutes rate;
     * tag "m15_rate" - fifteen-minutes rate;
     * Also do {@code consumeCounting}.
     */
    private static void consumeMetered(Metered metered, EventBuilder eventBuilder) {
        consumeCounting(metered, eventBuilder);
        eventBuilder.setTag("mean_rate", Variant.ofDouble(metered.getMeanRate()));
        eventBuilder.setTag("m1_rate", Variant.ofDouble(metered.getOneMinuteRate()));
        eventBuilder.setTag("m5_rate", Variant.ofDouble(metered.getFiveMinuteRate()));
        eventBuilder.setTag("m15_rate", Variant.ofDouble(metered.getFifteenMinuteRate()));
    }

    @FunctionalInterface
    private interface MetricContainerCreator<T extends Metric> {
        Event create(String name,
                     T t,
                     long timestamp,
                     String rateUnit,
                     String durationUnit);
    }

    @FunctionalInterface
    private interface MetricConsumer<T extends Metric> {
        void consume(T metric,
                     String rateUnit,
                     String durationUnit,
                     EventBuilder eventBuilder);
    }
}
