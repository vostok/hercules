package ru.kontur.vostok.hercules.graphite.sink;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.graphite.sink.converter.MetricConverter;
import ru.kontur.vostok.hercules.graphite.sink.converter.MetricEventConverter;
import ru.kontur.vostok.hercules.graphite.sink.converter.MetricWithTagsEventConverter;
import ru.kontur.vostok.hercules.health.AutoMetricStopwatch;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.health.Timer;
import ru.kontur.vostok.hercules.kafka.util.processing.BackendServiceFailedException;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.sink.ProcessorStatus;
import ru.kontur.vostok.hercules.sink.Sender;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.validation.IntegerValidators;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @deprecated use {@link GraphiteSender} instead
 */
@Deprecated
public class GraphiteEventSender extends Sender {
    private static final Logger LOGGER = LoggerFactory.getLogger(GraphiteEventSender.class);

    private final GraphitePinger graphitePinger;
    private final GraphiteClient graphiteClient;
    private final Timer graphiteClientTimer;
    private final MetricConverter metricsConverter;

    private final AtomicInteger sentMetricsCounter = new AtomicInteger(0);

    public GraphiteEventSender(Properties properties, MetricsCollector metricsCollector) {
        super(properties, metricsCollector);

        final String graphiteHost = PropertiesUtil.get(Props.GRAPHITE_HOST, properties).get();
        final int graphitePort = PropertiesUtil.get(Props.GRAPHITE_PORT, properties).get();
        final int retryLimit = PropertiesUtil.get(Props.RETRY_LIMIT, properties).get();
        final int diagnosticLogWritePeriodMs = PropertiesUtil.get(Props.DIAGNOSTIC_LOG_WRITE_PERIOD_MS, properties).get();
        final boolean graphiteTagsEnable = PropertiesUtil.get(Props.GRAPHITE_TAGS_ENABLE, properties).get();

        graphitePinger = new GraphitePinger(graphiteHost, graphitePort);
        graphiteClient = new GraphiteClient(graphiteHost, graphitePort, retryLimit);
        graphiteClientTimer = metricsCollector.timer("graphiteClientRequestTimeMs");
        metricsConverter = graphiteTagsEnable ? new MetricWithTagsEventConverter() : new MetricEventConverter();

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleWithFixedDelay(this::logSentMetricsCount,
                diagnosticLogWritePeriodMs,
                diagnosticLogWritePeriodMs,
                TimeUnit.MILLISECONDS);
    }

    @Override
    protected ProcessorStatus ping() {
        return graphitePinger.ping();
    }

    @Override
    protected int send(List<Event> events) throws BackendServiceFailedException {
        if (events.size() == 0) {
            return 0;
        }

        List<GraphiteMetricData> metricsToSend = events.stream()
                .map(metricsConverter::convert)
                .collect(Collectors.toList());

        if (metricsToSend.size() == 0) {
            return 0;
        }

        try (AutoMetricStopwatch ignored = new AutoMetricStopwatch(graphiteClientTimer, TimeUnit.MILLISECONDS)) {
            graphiteClient.send(metricsToSend);
        } catch (Exception exception) {
            throw new BackendServiceFailedException(exception);
        }

        sentMetricsCounter.addAndGet(metricsToSend.size());

        return metricsToSend.size();
    }

    @Override
    public boolean stop(long timeout, TimeUnit unit) {
        boolean stopped = super.stop(timeout, unit);

        graphiteClient.close();

        return stopped;
    }

    private void logSentMetricsCount() {
        LOGGER.info("Successfully sent {} metric(s) to Graphite.", sentMetricsCounter.getAndSet(0));
    }

    private static class Props {
        static final Parameter<String> GRAPHITE_HOST =
                Parameter.stringParameter("graphite.host").
                        required().
                        build();

        static final Parameter<Integer> GRAPHITE_PORT =
                Parameter.integerParameter("graphite.port").
                        required().
                        withValidator(IntegerValidators.portValidator()).
                        build();

        static final Parameter<Integer> RETRY_LIMIT =
                Parameter.integerParameter("retryLimit").
                        withDefault(3).
                        build();

        static final Parameter<Integer> DIAGNOSTIC_LOG_WRITE_PERIOD_MS =
                Parameter.integerParameter("diagnosticLogWritePeriodMs").
                        withDefault(60000).
                        build();

        static final Parameter<Boolean> GRAPHITE_TAGS_ENABLE =
                Parameter.booleanParameter("graphite.tags.enable").
                        withDefault(false).
                        build();
    }
}
