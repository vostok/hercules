package ru.kontur.vostok.hercules.graphite.sink;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.graphite.sink.connection.Channel;
import ru.kontur.vostok.hercules.graphite.sink.connection.EndpointException;
import ru.kontur.vostok.hercules.graphite.sink.converter.MetricConverter;
import ru.kontur.vostok.hercules.graphite.sink.converter.MetricEventConverter;
import ru.kontur.vostok.hercules.graphite.sink.converter.MetricWithTagsEventConverter;
import ru.kontur.vostok.hercules.health.AutoMetricStopwatch;
import ru.kontur.vostok.hercules.health.Counter;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.health.Timer;
import ru.kontur.vostok.hercules.kafka.util.processing.BackendServiceFailedException;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.sink.ProcessorStatus;
import ru.kontur.vostok.hercules.sink.parallel.sender.ParallelSender;
import ru.kontur.vostok.hercules.sink.parallel.sender.PreparedData;
import ru.kontur.vostok.hercules.util.metrics.GraphiteSanitizer;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Gregory Koshelev
 */
public class GraphiteSender extends ParallelSender<GraphiteSender.GraphitePreparedData> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GraphiteSender.class);

    private final int retryLimit;

    private final MetricConverter metricsConverter;
    private final GraphiteConnector connector;

    private final Timer sendMetricsTimeMsTimer;
    private final Counter retryCounter;

    public GraphiteSender(Properties properties, MetricsCollector metricsCollector) {
        super(properties, metricsCollector);

        this.retryLimit = PropertiesUtil.get(Props.RETRY_LIMIT, properties).get();

        final boolean graphiteTagsEnable = PropertiesUtil.get(Props.GRAPHITE_TAGS_ENABLE, properties).get();
        final boolean graphiteReplaceDots = PropertiesUtil.get(Props.GRAPHITE_REPLACE_DOTS, properties).get();
        GraphiteSanitizer sanitizer = graphiteReplaceDots ? GraphiteSanitizer.METRIC_NAME_SANITIZER : GraphiteSanitizer.METRIC_PATH_SANITIZER;
        this.metricsConverter = graphiteTagsEnable ? new MetricWithTagsEventConverter(sanitizer) : new MetricEventConverter(sanitizer);

        this.connector = new GraphiteConnector(PropertiesUtil.ofScope(properties, "graphite.connector"), retryLimit);

        this.sendMetricsTimeMsTimer = metricsCollector.timer("sendMetricsTimeMs");
        this.retryCounter = metricsCollector.counter("retryCount");
    }

    @Override
    public GraphitePreparedData prepare(List<Event> events) {
        List<String> metricsToSend = events.stream()
                .map(event -> format(metricsConverter.convert(event)))
                .collect(Collectors.toList());

        return new GraphitePreparedData(metricsToSend);
    }

    @Override
    public int send(GraphitePreparedData preparedData) throws BackendServiceFailedException {
        if (preparedData.getEventsCount() == 0) {
            return 0;
        }

        try (AutoMetricStopwatch ignored = new AutoMetricStopwatch(sendMetricsTimeMsTimer, TimeUnit.MILLISECONDS)) {
            sendMetrics(preparedData.metricsToSend);
        } catch (Exception exception) {
            throw new BackendServiceFailedException(exception);
        }

        return preparedData.getEventsCount();
    }

    @Override
    protected ProcessorStatus ping() {
        return connector.isReady() ? ProcessorStatus.AVAILABLE : ProcessorStatus.UNAVAILABLE;
    }

    private void sendMetrics(List<String> metrics) throws BackendServiceFailedException {
        Exception lastException;

        int attemptsLeft = retryLimit;
        do {
            boolean isRetry = attemptsLeft < retryLimit;
            try (Channel channel = connector.channel(isRetry)) {
                if (channel == null) {
                    throw new BackendServiceFailedException("There is no available endpoint");
                }
                channel.send(metrics);
                return;
            } catch (EndpointException ex) {
                if (attemptsLeft > 1) {
                    LOGGER.debug("Retry send exception, attempts left: " + (attemptsLeft - 1) + ", retry limit: " + retryLimit, ex);
                }
                retryCounter.increment();
                lastException = ex;
            }
        } while (--attemptsLeft > 0);

        throw new BackendServiceFailedException(lastException);
    }

    @Override
    public boolean stop(long timeout, TimeUnit unit) {
        boolean result = super.stop(timeout, unit);
        connector.close();
        return result;
    }

    public static class GraphitePreparedData implements PreparedData {
        private final List<String> metricsToSend;

        public GraphitePreparedData(List<String> metricsToSend) {
            this.metricsToSend = metricsToSend;
        }

        @Override
        public int getEventsCount() {
            return metricsToSend.size();
        }
    }

    static String format(GraphiteMetricData metric) {
        String value = FORMAT.get().format(metric.getMetricValue());
        return metric.getMetricName() + ' ' + value + ' ' + metric.getMetricUnixTime() + '\n';
    }

    private static final ThreadLocal<DecimalFormat> FORMAT = ThreadLocal.withInitial(() ->
            new DecimalFormat("0.000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH)));

    private static class Props {
        static final Parameter<Integer> RETRY_LIMIT =
                Parameter.integerParameter("retryLimit").
                        withDefault(3).
                        build();

        static final Parameter<Boolean> GRAPHITE_TAGS_ENABLE =
                Parameter.booleanParameter("graphite.tags.enable").
                        withDefault(false).
                        build();

        static final Parameter<Boolean> GRAPHITE_REPLACE_DOTS =
                Parameter.booleanParameter("graphite.replace.dots").
                        withDefault(false).
                        build();
    }
}
