package ru.kontur.vostok.hercules.sentry.sink;

import ru.kontur.vostok.hercules.health.Meter;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.health.MetricsUtil;
import ru.kontur.vostok.hercules.health.Timer;
import ru.kontur.vostok.hercules.kafka.util.processing.BackendServiceFailedException;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.routing.Router;
import ru.kontur.vostok.hercules.routing.sentry.SentryDestination;
import ru.kontur.vostok.hercules.sentry.client.SentryLevelParser;
import ru.kontur.vostok.hercules.sentry.client.impl.v9.SentryClientImplV9;
import ru.kontur.vostok.hercules.sentry.client.impl.v9.connector.SentryConnectorHolder;
import ru.kontur.vostok.hercules.sentry.client.SentryLevel;
import ru.kontur.vostok.hercules.sentry.client.SentryClient;
import ru.kontur.vostok.hercules.tags.LogEventTags;
import ru.kontur.vostok.hercules.util.concurrent.ThreadFactories;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.parameter.parsing.ParsingResult;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class SentrySyncProcessor {

    private static final SentryLevel defaultLevel = SentryLevel.ERROR;

    private final ScheduledExecutorService executor;
    private final MetricsCollector metricsCollector;
    private final Router<Event, SentryDestination> router;
    private final ConcurrentHashMap<String, Timer> eventProcessingTimerMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Meter> processedEventsMeterMap = new ConcurrentHashMap<>();
    private final SentryThrottlingService throttlingService;
    private final SentryClient sentryClient;
    private final SentryLevelParser sentryLevelParser;

    public SentrySyncProcessor(
            Properties sinkProperties,
            SentryConnectorHolder sentryConnectorHolder,
            MetricsCollector metricsCollector,
            Router<Event, SentryDestination> router,
            String herculesVersion
    ) {
        int retryLimit = PropertiesUtil.get(Props.RETRY_LIMIT, sinkProperties).get();
        this.metricsCollector = metricsCollector;
        this.router = router;

        long clientsUpdatePeriodMs = PropertiesUtil.get(Props.CLIENTS_UPDATE_PERIOD_MS, sinkProperties).get();
        this.executor = Executors.newSingleThreadScheduledExecutor(
                ThreadFactories.newDaemonNamedThreadFactory("sentry-clients-update"));
        this.executor.scheduleAtFixedRate(
                sentryConnectorHolder::update,
                clientsUpdatePeriodMs,
                clientsUpdatePeriodMs,
                TimeUnit.MILLISECONDS);

        Properties rateLimiterProperties = PropertiesUtil.ofScope(sinkProperties, "throttling.rate");
        this.throttlingService = new SentryThrottlingService(rateLimiterProperties, metricsCollector);
        this.sentryClient = new SentryClientImplV9(retryLimit,
                metricsCollector,
                sentryConnectorHolder,
                herculesVersion);
        this.sentryLevelParser = new SentryLevelParser();
    }

    /**
     * Process event
     *
     * @param event event
     * @return true if event is successfully processed
     * or returns false if event is invalid or non retryable error occurred
     * @throws BackendServiceFailedException if retryable error occurred on every attempt of retry,
     * error has recommended waiting time
     * or not described exception occurred
     */
    public boolean process(Event event) throws BackendServiceFailedException {
        final long sendingStart = System.currentTimeMillis();

        final long eventTimestampMs = TimeUtil.ticksToMillis(event.getTimestamp());

        if (!checkLevel(event)) {
            return false;
        }

        SentryDestination destination = router.route(event).sanitize();
        if (destination.isNowhere()) {
            return false;
        }

        if (!throttlingService.check(destination.organization(), eventTimestampMs)) {
            return false;
        }

        boolean processed = sentryClient.tryToSend(event, destination);
        final String prefix = getMetricsPrefix(destination);

        final long processingTimeMs = System.currentTimeMillis() - sendingStart;
        // TODO: 22.07.2022  move metrics to special class  
        eventProcessingTimerMap
                .computeIfAbsent(prefix, p -> metricsCollector.timer(p + ".eventProcessingTimeMs"))
                .update(processingTimeMs, TimeUnit.MILLISECONDS);
        if (processed) {
            processedEventsMeterMap
                    .computeIfAbsent(prefix, p -> metricsCollector.meter(p + ".processedEvents"))
                    .mark();
        }

        return processed;
    }

    private boolean checkLevel(Event event) {
        return  ContainerUtil.extract(event.getPayload(), LogEventTags.LEVEL_TAG)
                .map(sentryLevelParser::parse)
                .filter(ParsingResult::hasValue)
                .map(ParsingResult::get)
                .map(value -> defaultLevel.compareTo(value) >= 0)
                .orElse(false);
    }

    public void stop() {
        executor.shutdown();
    }

    private static class Props {
        static final Parameter<Integer> RETRY_LIMIT =
                Parameter.integerParameter("sentry.retryLimit").
                        withDefault(3).
                        build();
        static final Parameter<Long> CLIENTS_UPDATE_PERIOD_MS =
                Parameter.longParameter("sentry.clientsUpdatePeriodMs").
                        withDefault(1_000L * 60 * 60).
                        build();
    }

    private String getMetricsPrefix(SentryDestination destination) {
            return MetricsUtil.toMetricPath("byOrgsAndProjects", destination.organization(), destination.project());
    }
}
