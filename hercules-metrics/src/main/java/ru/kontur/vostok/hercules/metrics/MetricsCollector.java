package ru.kontur.vostok.hercules.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

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
        String graphiteServerAddr = properties.getProperty("graphite.server.addr", "localhost");
        int graphiteServerPort = PropertiesUtil.get(properties, "graphite.server.port", 2003);
        String prefix = properties.getProperty("graphite.prefix");
        long period = PropertiesUtil.get(properties, "period", 60);

        this.period = period;

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
     * Get thoughput meter by the name
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
}