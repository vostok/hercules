package ru.kontur.vostok.hercules.micrometer.registry;

import com.codahale.metrics.*;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.encoder.EventBuilder;
import ru.kontur.vostok.hercules.uuid.UuidGenerator;

import java.util.HashMap;
import java.util.Map;

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
                        (Gauge<?>) metric,
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

    private static <T extends Metric> Event formatMetric(String name,
                                                         T metric,
                                                         long timestamp,
                                                         String rateUnit,
                                                         String durationUnit,
                                                         MetricConsumer<T> consumer) {
        EventBuilder eventBuilder = new EventBuilder();
        eventBuilder.setVersion(1);
        //TODO: generate event id
        eventBuilder.setEventId(GENERATOR.next());

        eventBuilder.setTag("timestamp", Variant.ofLong(timestamp));
        eventBuilder.setTag("name", Variant.ofString(name));
        consumer.consume(metric, rateUnit, durationUnit, eventBuilder);

        return eventBuilder.build();
    }

    private static void consumeGauge(Gauge<?> gauge,
                                     String rateUnit,
                                     String durationUnit,
                                     EventBuilder eventBuilder) {
        eventBuilder.setTag("value", Variant.ofString(String.valueOf(gauge.getValue())));
    }

    private static void consumeCounter(Counter counter,
                                       String rateUnit,
                                       String durationUnit,
                                       EventBuilder eventBuilder) {
        consumeCounting(counter, eventBuilder);
    }

    private static void consumeHistogram(Histogram histogram,
                                         String rateUnit,
                                         String durationUnit,
                                         EventBuilder eventBuilder) {
        consumeCounting(histogram, eventBuilder);
        consumeSampling(histogram, eventBuilder);
    }

    private static void consumeMeter(Meter meter,
                                     String rateUnit,
                                     String durationUnit,
                                     EventBuilder eventBuilder) {
        consumeMetered(meter, eventBuilder);
        eventBuilder.setTag("rate_unit", Variant.ofString(rateUnit));
    }

    private static void consumeTimer(Timer timer,
                                     String rateUnit,
                                     String durationUnit,
                                     EventBuilder eventBuilder) {
        consumeMetered(timer, eventBuilder);
        consumeSampling(timer, eventBuilder);
        eventBuilder.setTag("rate_unit", Variant.ofString(rateUnit));
        eventBuilder.setTag("duration_unit", Variant.ofString(durationUnit));
    }

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

    private static void consumeCounting(Counting counting, EventBuilder eventBuilder) {
        eventBuilder.setTag("count", Variant.ofLong(counting.getCount()));
    }

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
