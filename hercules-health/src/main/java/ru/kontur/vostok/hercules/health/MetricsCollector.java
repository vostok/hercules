package ru.kontur.vostok.hercules.health;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import ru.kontur.vostok.hercules.application.Application;
import ru.kontur.vostok.hercules.application.ApplicationContext;
import ru.kontur.vostok.hercules.util.lifecycle.Lifecycle;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.validation.IntegerValidators;

import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * @author Gregory Koshelev
 */
public class MetricsCollector implements IMetricsCollector, Lifecycle {
    private final MetricRegistry registry = new MetricRegistry();

    private final long period;

    private final GraphiteReporter graphiteReporter;

    /**
     *
     */
    public MetricsCollector(Properties properties) {
        String graphiteServerAddr = PropertiesUtil.get(Props.GRAPHITE_SERVER, properties).get();
        int graphiteServerPort = PropertiesUtil.get(Props.GRAPHITE_PORT, properties).get();

        ApplicationContext context = Application.context();
        String prefix = String.join(".",
                PropertiesUtil.get(Props.GRAPHITE_PREFIX, properties).get(),
                context.getApplicationId(),
                context.getEnvironment(),
                context.getZone(),
                context.getInstanceId()
        );

        this.period = PropertiesUtil.get(Props.REPORT_PERIOD_SECONDS, properties).get();

        Graphite graphite = new Graphite(new InetSocketAddress(graphiteServerAddr, graphiteServerPort));
        graphiteReporter = GraphiteReporter.forRegistry(registry)
                .prefixedWith(prefix)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .filter(MetricFilter.ALL)
                .build(graphite);
    }

    /**
     * Start to report metrics to the Graphite
     */
    @Override
    public void start() {
        graphiteReporter.start(period, TimeUnit.SECONDS);
    }

    /**
     * Stop to report metrics to the Graphite
     */
    @Override
    public boolean stop(long timeout, TimeUnit unit) {
        graphiteReporter.stop();
        return true;//FIXME: Replace with CompletableFuture + orTimeout when migrate to Java 11.
    }

    /**
     * @deprecated use {@link #stop(long, TimeUnit)} instead
     */
    @Deprecated
    public void stop() {
        stop(0, TimeUnit.MILLISECONDS);
    }

    /**
     * Get throughput meter by the name
     *
     * @param name the name of the metric
     * @return requested meter
     */
    public Meter meter(String name) {
        return new MeterImpl(registry.meter(name), name);
    }

    /**
     * Get timer by the name
     *
     * @param name the name of the timer
     * @return requested timer
     */
    public Timer timer(String name) {
        return new TimerImpl(registry.timer(name));
    }

    /**
     * Get counter by the name
     *
     * @param name the name of the counter
     * @return requested counter
     */
    public Counter counter(String name) {
        return new CounterImpl(registry.counter(name));
    }

    /**
     * Get histogram by the name
     *
     * @param name the name of the histogram
     * @return requested histogram
     */
    public Histogram histogram(String name) {
        return new HistogramImpl(registry.histogram(name));
    }

    /**
     * Register metric by the name with custom function
     *
     * @param name     the name of the metric
     * @param supplier the custom function to provide metric's values
     * @param <T>      the metric's value type (ordinarily Integer or Long)
     */
    public <T> void gauge(String name, Supplier<T> supplier) {
        registry.register(name, (Gauge<T>) supplier::get);
    }

    /**
     * Removes the metric with the given name
     *
     * @param name the name of the metric
     * @return whether or not the metric was removed
     */
    public boolean remove(String name) {
        return registry.remove(name);
    }

    /**
     * Creates HTTP Metric for the handler.
     *
     * @param name the handler name
     * @return HTTP Metric for the handler
     */
    public HttpMetric http(String name) {
        return new HttpMetric(name, this);
    }

    private static class Props {
        static final Parameter<String> GRAPHITE_SERVER =
                Parameter.stringParameter("graphite.server.addr").
                        withDefault("localhost").
                        build();

        static final Parameter<Integer> GRAPHITE_PORT =
                Parameter.integerParameter("graphite.server.port").
                        withDefault(2003).
                        withValidator(IntegerValidators.portValidator()).
                        build();

        static final Parameter<String> GRAPHITE_PREFIX =
                Parameter.stringParameter("graphite.prefix").
                        required().
                        build();

        static final Parameter<Integer> REPORT_PERIOD_SECONDS =
                Parameter.integerParameter("period").
                        withDefault(60).
                        withValidator(IntegerValidators.positive()).
                        build();
    }

    /**
     * @author Gregory Koshelev
     */
    public static class MeterImpl implements Meter {
        private final com.codahale.metrics.Meter meter;
        private final String name;

        MeterImpl(com.codahale.metrics.Meter meter, String name) {
            this.meter = meter;
            this.name = name;
        }

        @Override
        public void mark(long n) {
            meter.mark(n);
        }

        @Override
        public void mark() {
            meter.mark();
        }

        @Override
        public String name() {
            return name;
        }
    }

    /**
     * @author Gregory Koshelev
     */
    public static class TimerImpl implements Timer {
        private final com.codahale.metrics.Timer timer;

        TimerImpl(com.codahale.metrics.Timer timer) {
            this.timer = timer;
        }

        @Override
        public void update(long duration, TimeUnit unit) {
            timer.update(duration, unit);
        }
    }

    /**
     * @author Gregory Koshelev
     */
    public static class CounterImpl implements Counter {
        private final com.codahale.metrics.Counter counter;

        CounterImpl(com.codahale.metrics.Counter counter) {
            this.counter = counter;
        }

        @Override
        public void increment(long value) {
            counter.inc(value);
        }

        @Override
        public void decrement(long value) {
            counter.dec(value);
        }
    }

    /**
     * @author Gregory Koshelev
     */
    public static class HistogramImpl implements Histogram {
        private final com.codahale.metrics.Histogram histogram;

        HistogramImpl(com.codahale.metrics.Histogram histogram) {
            this.histogram = histogram;
        }

        @Override
        public void update(int value) {
            histogram.update(value);
        }

        @Override
        public void update(long value) {
            histogram.update(value);
        }
    }
}
