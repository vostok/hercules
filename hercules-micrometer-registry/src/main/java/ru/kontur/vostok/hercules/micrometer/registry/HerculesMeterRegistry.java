package ru.kontur.vostok.hercules.micrometer.registry;

import com.codahale.metrics.MetricRegistry;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.dropwizard.DropwizardClock;
import io.micrometer.core.instrument.dropwizard.DropwizardMeterRegistry;
import io.micrometer.core.instrument.util.HierarchicalNameMapper;
import io.micrometer.core.lang.Nullable;
import java.util.concurrent.TimeUnit;

/**
 * @author Daniil Zhenikhov
 */
public class HerculesMeterRegistry extends DropwizardMeterRegistry {
    private final HerculesMetricConfig config;
    private final HerculesMetricReporter reporter;

    public HerculesMeterRegistry(HerculesMetricConfig config, Clock clock) {
        this(config, clock, HierarchicalNameMapper.DEFAULT);
    }

    public HerculesMeterRegistry(HerculesMetricConfig config,
                                 Clock clock,
                                 HierarchicalNameMapper nameMapper) {
        this(config, clock, nameMapper, new MetricRegistry());
    }

    public HerculesMeterRegistry(HerculesMetricConfig config,
                                 Clock clock,
                                 HierarchicalNameMapper hierarchicalNameMapper,
                                 MetricRegistry metricRegistry) {
        this(config,
                clock,
                hierarchicalNameMapper,
                metricRegistry,
                defaultHerculesMetricReporter(config, clock, metricRegistry));
    }

    public HerculesMeterRegistry(HerculesMetricConfig config,
                                 Clock clock,
                                 HierarchicalNameMapper nameMapper,
                                 MetricRegistry metricRegistry,
                                 HerculesMetricReporter reporter) {
        super(config, metricRegistry, nameMapper, clock);

        this.config = config;
        this.reporter = reporter;

        if (config.enabled()) {
            start();
        }
    }

    /**
     * A build {@link HerculesMetricReporter default HerculesMetricReporter} instances. Defaults using the
     * config eventQueue, {@link DropwizardClock DropwizardClock}, config rateUnit and
     * config durationUnits.
     */
    private static HerculesMetricReporter defaultHerculesMetricReporter(HerculesMetricConfig config,
                                                                        Clock clock,
                                                                        MetricRegistry metricRegistry) {
        return HerculesMetricReporter.forRegistry(metricRegistry)
                .registerStream(config.eventQueue())
                .withClock(new DropwizardClock(clock))
                .convertRatesTo(config.rateUnits())
                .convertDurationsTo(config.durationUnits())
                .build();
    }

    public void stop() {
        this.reporter.stop();
    }

    public void start() {
        this.reporter.start(config.step().getSeconds(), TimeUnit.SECONDS);
    }

    @Override
    public void close() {
        reporter.report();
        stop();
        super.close();
    }

    @Override
    @Nullable
    protected Double nullGaugeValue() {
        return null;
    }
}
