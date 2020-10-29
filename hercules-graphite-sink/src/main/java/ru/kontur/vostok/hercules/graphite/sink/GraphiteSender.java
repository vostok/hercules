package ru.kontur.vostok.hercules.graphite.sink;

import ru.kontur.vostok.hercules.graphite.sink.connection.Channel;
import ru.kontur.vostok.hercules.graphite.sink.connection.EndpointException;
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

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * @author Gregory Koshelev
 */
public class GraphiteSender extends Sender {
    private final int retryLimit;

    private final MetricConverter metricsConverter;
    private final GraphiteConnector connector;

    private final Timer sendMetricsTimeMsTimer;

    private final AtomicLong sentMetricsCounter = new AtomicLong(0);

    public GraphiteSender(Properties properties, MetricsCollector metricsCollector) {
        super(properties, metricsCollector);

        this.retryLimit = PropertiesUtil.get(Props.RETRY_LIMIT, properties).get();

        final boolean graphiteTagsEnable = PropertiesUtil.get(Props.GRAPHITE_TAGS_ENABLE, properties).get();
        this.metricsConverter = graphiteTagsEnable ? new MetricWithTagsEventConverter() : new MetricEventConverter();

        this.connector = new GraphiteConnector(PropertiesUtil.ofScope(properties, "graphite.connector"));

        this.sendMetricsTimeMsTimer = metricsCollector.timer("sendMetricsTimeMs");
    }

    @Override
    protected int send(List<Event> events) throws BackendServiceFailedException {
        if (events.size() == 0) {
            return 0;
        }

        List<GraphiteMetricData> metricsToSend = events.stream()
                .map(metricsConverter::convert)
                .collect(Collectors.toList());

        try (AutoMetricStopwatch ignored = new AutoMetricStopwatch(sendMetricsTimeMsTimer, TimeUnit.MILLISECONDS)) {
            send(metricsToSend, retryLimit);
        } catch (Exception exception) {
            throw new BackendServiceFailedException(exception);
        }

        sentMetricsCounter.addAndGet(metricsToSend.size());
        return metricsToSend.size();
    }

    @Override
    protected ProcessorStatus ping() {
        return connector.isReady() ? ProcessorStatus.AVAILABLE : ProcessorStatus.UNAVAILABLE;
    }

    private void send(List<GraphiteMetricData> metrics, int attempts) throws EndpointException, IOException {
        IOException lastException;

        int attemptsLeft = attempts;
        do {
            try (Channel channel = connector.channel()) {
                if (channel == null) {
                    throw new EndpointException("There is no available endpoint");
                }
                try {
                    channel.send(metrics);
                    return;
                } catch (IOException ex) {
                    lastException = ex;
                }
            }

        } while (--attemptsLeft > 0);

        throw lastException;
    }

    @Override
    public boolean stop(long timeout, TimeUnit unit) {
        boolean result = super.stop(timeout, unit);
        connector.close();
        return result;
    }

    private static class Props {
        static final Parameter<Integer> RETRY_LIMIT =
                Parameter.integerParameter("retryLimit").
                        withDefault(3).
                        build();

        static final Parameter<Boolean> GRAPHITE_TAGS_ENABLE =
                Parameter.booleanParameter("graphite.tags.enable").
                        withDefault(false).
                        build();

    }
}
