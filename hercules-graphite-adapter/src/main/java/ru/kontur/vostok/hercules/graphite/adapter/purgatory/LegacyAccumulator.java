package ru.kontur.vostok.hercules.graphite.adapter.purgatory;

import ru.kontur.vostok.hercules.gate.client.EventPublisher;
import ru.kontur.vostok.hercules.gate.client.EventPublisherFactory;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.health.MetricsUtil;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * This accumulator uses {@link EventPublisher} to send events to the Gate.
 * <p>
 * TODO: should be rewritten: don't use EventPublisher since it has bad design and performance.
 *
 * @author Gregory Koshelev
 */
public class LegacyAccumulator implements Accumulator {
    private static final String METRICS_SCOPE = LegacyAccumulator.class.getSimpleName();

    private final EventPublisher eventPublisher = EventPublisherFactory.getInstance();

    private final int periodMs;
    private final int capacity;
    private final int batchSize;

    private final String plainMetricsStream;
    private final String taggedMetricsStream;

    private final MetricsCollector metricsCollector;

    public LegacyAccumulator(Properties properties, MetricsCollector metricsCollector) {
        periodMs = PropertiesUtil.get(Props.PERIOD_MS, properties).get();
        capacity = PropertiesUtil.get(Props.CAPACITY, properties).get();
        batchSize = PropertiesUtil.get(Props.BATCH_SIZE, properties).get();

        //FIXME: Remove these properties after refactoring
        plainMetricsStream = PropertiesUtil.get(Props.PLAIN_METRICS_STREAM, properties).get();
        taggedMetricsStream = PropertiesUtil.get(Props.TAGGED_METRICS_STREAM, properties).get();

        this.metricsCollector = metricsCollector;
    }

    @Override
    public void start() {
        eventPublisher.register(plainMetricsStream, plainMetricsStream, periodMs, capacity, batchSize, false);
        eventPublisher.register(taggedMetricsStream, taggedMetricsStream, periodMs, capacity, batchSize, false);

        metricsCollector.gauge(MetricsUtil.toMetricPathWithPrefix(METRICS_SCOPE, "queueCapacity"), () -> capacity);
        metricsCollector.gauge(MetricsUtil.toMetricPathWithPrefix(METRICS_SCOPE, "plainMetricsQueueSize"), () -> eventPublisher.size(plainMetricsStream));
        metricsCollector.gauge(MetricsUtil.toMetricPathWithPrefix(METRICS_SCOPE, "taggedMetricsQueueSize"), () -> eventPublisher.size(taggedMetricsStream));
    }

    @Override
    public boolean stop(long timeout, TimeUnit timeUnit) {
        return eventPublisher.stop(timeout, timeUnit);
    }

    public void add(String stream, Event event) {
        eventPublisher.publish(stream, event);
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

        static Parameter<Integer> PERIOD_MS =
                Parameter.integerParameter("period.ms").
                        withDefault(10_000).
                        build();

        static Parameter<Integer> CAPACITY =
                Parameter.integerParameter("capacity").
                        withDefault(100_000_000).
                        build();

        static Parameter<Integer> BATCH_SIZE =
                Parameter.integerParameter("batch.size").
                        withDefault(10_000).
                        build();
    }
}
