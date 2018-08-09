package ru.kontur.vostok.hercules.micrometer.registry;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricAttribute;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Timer;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import ru.kontur.vostok.hercules.gateway.client.EventPublisher;
import ru.kontur.vostok.hercules.gateway.client.EventPublisherFactory;
import ru.kontur.vostok.hercules.gateway.client.EventQueue;
import ru.kontur.vostok.hercules.protocol.Event;

/**
 * A reporter which publishes metric values to a Hercules gateway.
 * Filtering will be applying in the base class {@link com.codahale.metrics.ScheduledReporter}.
 *
 * @author Daniil Zhenikhov
 */
public class HerculesMetricReporter extends ScheduledReporter {
    private static final String NAME = "hercules-metric-reporter";

    private final HerculesMetricFormatter herculesMetricFormatter;
    private final EventPublisher eventPublisher;
    private final EventQueue eventQueue;
    private final Clock clock;

    protected HerculesMetricReporter(MetricRegistry registry,
                                     Clock clock,
                                     EventQueue eventQueue,
                                     TimeUnit rateUnit,
                                     TimeUnit durationUnit,
                                     MetricFilter filter,
                                     ScheduledExecutorService executor,
                                     boolean shutdownExecutorOnStop,
                                     Set<MetricAttribute> disabledMetricAttributes) {
        super(registry, NAME, filter, rateUnit,
                durationUnit, executor, shutdownExecutorOnStop, disabledMetricAttributes);

        this.clock = clock;
        this.eventQueue = eventQueue;
        this.eventPublisher = EventPublisherFactory.getInstance();
        this.herculesMetricFormatter = new HerculesMetricFormatter(
                getRateUnit(),
                getDurationUnit(),
                disabledMetricAttributes);

        eventPublisher.register(this.eventQueue);
    }

    /**
     * Returns a new {@link HerculesMetricReporter.Builder} for {@link HerculesMetricReporter}.
     *
     * @param registry the registry to report
     * @return a {@link HerculesMetricReporter.Builder} instance for a {@link HerculesMetricReporter}
     */
    public static HerculesMetricReporter.Builder forRegistry(MetricRegistry registry) {
        return new HerculesMetricReporter.Builder(registry);
    }

    @Override
    public void start(long period, TimeUnit timeUnit) {
        eventPublisher.start();
        super.start(period, timeUnit);
    }

    @Override
    public void stop() {
        try {
            super.stop();
        } finally {
            eventPublisher.stop(1000);
        }
    }

    @Override
    public void report(SortedMap<String, Gauge> gauges,
                       SortedMap<String, Counter> counters,
                       SortedMap<String, Histogram> histograms,
                       SortedMap<String, Meter> meters,
                       SortedMap<String, Timer> timers) {
        final long timestamp = TimeUnit.MILLISECONDS.toSeconds(clock.getTime());

        for (Map.Entry<String, Gauge> entry : gauges.entrySet()) {
            Event event = herculesMetricFormatter.formatGauge(entry.getKey(), entry.getValue(), timestamp);
            eventPublisher.publish(eventQueue.getName(), event);
        }

        for (Map.Entry<String, Counter> entry : counters.entrySet()) {
            Event event = herculesMetricFormatter.formatCounter(entry.getKey(), entry.getValue(), timestamp);
            eventPublisher.publish(eventQueue.getName(), event);
        }

        for (Map.Entry<String, Histogram> entry : histograms.entrySet()) {
            Event event = herculesMetricFormatter.formatHistogram(entry.getKey(), entry.getValue(), timestamp);
            eventPublisher.publish(eventQueue.getName(), event);
        }

        for (Map.Entry<String, Meter> entry : meters.entrySet()) {
            Event event = herculesMetricFormatter.formatMeter(entry.getKey(), entry.getValue(), timestamp);
            eventPublisher.publish(eventQueue.getName(), event);
        }

        for (Map.Entry<String, Timer> entry : timers.entrySet()) {
            Event event = herculesMetricFormatter.formatTimer(entry.getKey(), entry.getValue(), timestamp);
            eventPublisher.publish(eventQueue.getName(), event);
        }
    }

    /**
     * A builder for {@link HerculesMetricReporter} instances. Defaults using the
     * default clock, converting rates to events/second, converting durations to milliseconds, and
     * not filtering metrics.
     */
    public static class Builder {
        private final MetricRegistry registry;
        private Clock clock;
        private TimeUnit rateUnit;
        private TimeUnit durationUnit;
        private MetricFilter filter;
        private ScheduledExecutorService executor;
        private boolean shutdownExecutorOnStop;
        private Set<MetricAttribute> disabledMetricAttributes;
        private EventQueue eventQueue;

        private Builder(MetricRegistry registry) {
            this.registry = registry;
            this.clock = Clock.defaultClock();
            this.rateUnit = TimeUnit.SECONDS;
            this.durationUnit = TimeUnit.MILLISECONDS;
            this.filter = MetricFilter.ALL;
            this.executor = null;
            this.shutdownExecutorOnStop = true;
            this.disabledMetricAttributes = Collections.emptySet();
            this.eventQueue = null;
        }

        /**
         * Specifies whether or not, the executor (used for reporting) will be stopped with same time with reporter.
         * Default value is true.
         * Setting this parameter to false, has the sense in combining with providing external managed executor via {@link #scheduleOn(ScheduledExecutorService)}.
         *
         * @param shutdownExecutorOnStop if true, then executor will be stopped in same time with this reporter
         * @return {@code this}
         */
        public HerculesMetricReporter.Builder shutdownExecutorOnStop(boolean shutdownExecutorOnStop) {
            this.shutdownExecutorOnStop = shutdownExecutorOnStop;
            return this;
        }

        /**
         * Specifies the executor to use while scheduling reporting of metrics.
         * Default value is null.
         * Null value leads to executor will be auto created on start.
         *
         * @param executor the executor to use while scheduling reporting of metrics.
         * @return {@code this}
         */
        public HerculesMetricReporter.Builder scheduleOn(ScheduledExecutorService executor) {
            this.executor = executor;
            return this;
        }

        /**
         * Use the given {@link Clock} instance for the time.
         *
         * @param clock a {@link Clock} instance
         * @return {@code this}
         */
        public HerculesMetricReporter.Builder withClock(Clock clock) {
            this.clock = clock;
            return this;
        }

        /**
         * Register event queue in event publisher
         *
         * @param eventQueue the event queue for registration to event publisher
         * @return {@code this}
         */
        public HerculesMetricReporter.Builder registerStream(EventQueue eventQueue) {
            this.eventQueue = eventQueue;
            return this;
        }

        /**
         * Convert rates to the given time unit.
         *
         * @param rateUnit a unit of time
         * @return {@code this}
         */
        public HerculesMetricReporter.Builder convertRatesTo(TimeUnit rateUnit) {
            this.rateUnit = rateUnit;
            return this;
        }

        /**
         * Convert durations to the given time unit.
         *
         * @param durationUnit a unit of time
         * @return {@code this}
         */
        public HerculesMetricReporter.Builder convertDurationsTo(TimeUnit durationUnit) {
            this.durationUnit = durationUnit;
            return this;
        }

        /**
         * Only report metrics which match the given filter.
         *
         * @param filter a {@link MetricFilter}
         * @return {@code this}
         */
        public HerculesMetricReporter.Builder filter(MetricFilter filter) {
            this.filter = filter;
            return this;
        }

        /**
         * Don't report the passed metric attributes for all metrics.
         * See {@link MetricAttribute}.
         *
         * @param disabledMetricAttributes a {@link MetricFilter}
         * @return {@code this}
         */
        public HerculesMetricReporter.Builder disabledMetricAttributes(Set<MetricAttribute> disabledMetricAttributes) {
            this.disabledMetricAttributes = disabledMetricAttributes;
            return this;
        }

        /**
         * Builds a {@link HerculesMetricReporter} with the given properties, sending metrics using the
         * given {@link EventPublisher}.
         *
         * @return a {@link HerculesMetricReporter}
         */
        public HerculesMetricReporter build() {
            if (Objects.isNull(eventQueue)) {
                throw new IllegalStateException("Kafka stream is null");
            }

            return new HerculesMetricReporter(registry,
                    clock,
                    eventQueue,
                    rateUnit,
                    durationUnit,
                    filter,
                    executor,
                    shutdownExecutorOnStop,
                    disabledMetricAttributes);
        }
    }
}