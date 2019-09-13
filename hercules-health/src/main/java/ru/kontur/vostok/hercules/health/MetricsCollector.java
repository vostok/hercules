package ru.kontur.vostok.hercules.health;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import ru.kontur.vostok.hercules.util.application.ApplicationContext;
import ru.kontur.vostok.hercules.util.application.ApplicationContextHolder;
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
public class MetricsCollector {
    private MetricRegistry registry = new MetricRegistry();

    private final long period;

    private final Graphite graphite;
    private final GraphiteReporter graphiteReporter;

    /**
     *
     */
    public MetricsCollector(Properties properties) {
        String graphiteServerAddr = PropertiesUtil.get(Props.GRAPHITE_SERVER, properties).get();
        int graphiteServerPort = PropertiesUtil.get(Props.GRAPHITE_PORT, properties).get();

        ApplicationContext applicationContext = ApplicationContextHolder.get();
        String prefix = String.join(".",
                PropertiesUtil.get(Props.GRAPHITE_PREFIX, properties).get(),
                applicationContext.getApplicationId(),
                applicationContext.getEnvironment(),
                applicationContext.getZone(),
                applicationContext.getInstanceId()
        );

        this.period = PropertiesUtil.get(Props.REPORT_PERIOD_SECONDS, properties).get();

        graphite = new Graphite(new InetSocketAddress(graphiteServerAddr, graphiteServerPort));
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
    public void start() {
        graphiteReporter.start(period, TimeUnit.SECONDS);
    }

    /**
     * Stop to report metrics to the Graphite
     */
    public void stop() {
        graphiteReporter.stop();
    }

    /**
     * Get throughput meter by the name
     * @param name is the name of the metric
     * @return requested meter
     */
    public Meter meter(String name) {
        return registry.meter(name);
    }

    /**
     * Get counter by the name
     * @param name is the name of the counter
     * @return requested counter
     */
    public Counter counter(String name) {
        return registry.counter(name);
    }

    /**
     * Get timer by the name
     * @param name is the name of the timer
     * @return requested timer
     */
    public Timer timer(String name) {
        return registry.timer(name);
    }

    /**
     * Register metric by the name with custom function
     * @param name is the name of the metric
     * @param supplier is the custom function to provide metric's values
     * @param <T> is the metric's value type (ordinarily Integer or Long)
     */
    public <T> void gauge(String name, Supplier<T> supplier) {
        registry.register(name, (Gauge<T>) supplier::get);
    }

    /**
     * Removes the metric with the given name
     * @param name is the name of the metric
     * @return whether or not the metric was removed
     */
    public boolean remove(String name) { return registry.remove(name); }

    /**
     * Get histogram by the name
     * @param name is the name of the histogram
     * @return requested histogram
     */
    public Histogram histogram(String name) {
        return registry.histogram(name);
    }

    /**
     * Create HttpMetrics aggregation object
     * @param name handler name
     * @return HttpMetrics object
     */
    public HttpMetrics httpMetrics(String name) {
        return new HttpMetrics(name, this);
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
}
