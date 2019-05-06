package ru.kontur.vostok.hercules.graphite;

import com.codahale.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.health.AutoMetricStopwatch;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.kafka.util.processing.BackendServiceFailedException;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.format.EventFormatter;
import ru.kontur.vostok.hercules.sink.Sender;
import ru.kontur.vostok.hercules.sink.SenderStatus;
import ru.kontur.vostok.hercules.util.logging.LoggingConstants;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;
import ru.kontur.vostok.hercules.util.validation.Validators;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class GraphiteEventSender extends Sender {
    private static final Logger LOGGER = LoggerFactory.getLogger(GraphiteEventSender.class);
    private static final Logger RECEIVED_EVENT_LOGGER = LoggerFactory.getLogger(LoggingConstants.RECEIVED_EVENT_LOGGER_NAME);
    private static final Logger PROCESSED_EVENT_LOGGER = LoggerFactory.getLogger(LoggingConstants.PROCESSED_EVENT_LOGGER_NAME);
    private static final Logger DROPPED_EVENT_LOGGER = LoggerFactory.getLogger(LoggingConstants.DROPPED_EVENT_LOGGER_NAME);

    private final GraphitePinger graphitePinger;
    private final GraphiteClient graphiteClient;
    private final Timer graphiteClientTimer;

    public GraphiteEventSender(Properties properties, MetricsCollector metricsCollector) {
        super(properties, metricsCollector);

        final String graphiteHost = Props.GRAPHITE_HOST.extract(properties);
        final int graphitePort = Props.GRAPHITE_PORT.extract(properties);
        final int retryLimit = Props.RETRY_LIMIT.extract(properties);

        graphitePinger = new GraphitePinger(graphiteHost, graphitePort);
        graphiteClient = new GraphiteClient(graphiteHost, graphitePort, retryLimit);
        graphiteClientTimer = metricsCollector.timer("graphiteClientRequestTimeMs");
    }

    @Override
    protected SenderStatus ping() {
        return graphitePinger.ping();
    }

    @Override
    protected int send(List<Event> events) throws BackendServiceFailedException {
        if (events.size() == 0)
            return 0;

        if (RECEIVED_EVENT_LOGGER.isTraceEnabled()) {
            events.forEach(event -> RECEIVED_EVENT_LOGGER.trace("{},{}", event.getTimestamp(), event.getUuid()));
        }

        List<GraphiteMetricData> metricsToSend = events.stream()
                .filter(this::Validate)
                .map(MetricEventConverter::convert)
                .collect(Collectors.toList());

        if (metricsToSend.size() == 0)
            return 0;

        try (AutoMetricStopwatch ignored = new AutoMetricStopwatch(graphiteClientTimer, TimeUnit.MILLISECONDS)) {
            graphiteClient.send(metricsToSend);
        } catch (Exception error) {
            throw new BackendServiceFailedException(error);
        }

        if (PROCESSED_EVENT_LOGGER.isTraceEnabled()) {
            events.forEach(event -> PROCESSED_EVENT_LOGGER.trace("{},{}", event.getTimestamp(), event.getUuid()));
        }

        return metricsToSend.size();
    }

    private boolean Validate(Event event) {
        if (MetricEventFilter.isValid(event))
            return true;

        DROPPED_EVENT_LOGGER.trace("{},{}", event.getTimestamp(), event.getUuid());

        if (event != null)
            LOGGER.warn("Invalid metric event: {}", EventFormatter.format(event, true));

        return false;
    }

    private static class Props {
        static final PropertyDescription<String> GRAPHITE_HOST = PropertyDescriptions
                .stringProperty("graphite.host")
                .build();

        static final PropertyDescription<Integer> GRAPHITE_PORT = PropertyDescriptions
                .integerProperty("graphite.port")
                .withValidator(Validators.portValidator())
                .build();

        static final PropertyDescription<Integer> RETRY_LIMIT = PropertyDescriptions
                .integerProperty("retryLimit")
                .withDefaultValue(3)
                .build();
    }
}
