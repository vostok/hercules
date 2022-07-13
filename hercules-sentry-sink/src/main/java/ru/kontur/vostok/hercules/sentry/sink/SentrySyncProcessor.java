package ru.kontur.vostok.hercules.sentry.sink;

import io.sentry.SentryClient;
import io.sentry.connection.ConnectionException;
import io.sentry.connection.TooManyRequestsException;
import io.sentry.dsn.InvalidDsnException;
import io.sentry.event.Event.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.health.Meter;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.health.Timer;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.kafka.util.processing.BackendServiceFailedException;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.routing.Router;
import ru.kontur.vostok.hercules.routing.sentry.SentryDestination;
import ru.kontur.vostok.hercules.sentry.sink.converters.SentryEventConverter;
import ru.kontur.vostok.hercules.sentry.sink.converters.SentryLevelEnumParser;
import ru.kontur.vostok.hercules.tags.LogEventTags;
import ru.kontur.vostok.hercules.util.concurrent.ThreadFactories;
import ru.kontur.vostok.hercules.util.functional.Result;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(SentrySyncProcessor.class);

    private final Level defaultLevel = Level.WARNING;
    private final int retryLimit;
    private final SentryClientHolder sentryClientHolder;
    private final ScheduledExecutorService executor;
    private final SentryEventConverter eventConverter;
    private final MetricsCollector metricsCollector;
    private final Router<Event, SentryDestination> router;
    private final ConcurrentHashMap<String, Timer> eventProcessingTimerMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Meter> processedEventsMeterMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Meter> throttledBySentryEventsMeterMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Meter> errorTypesMeterMap = new ConcurrentHashMap<>();

    private final SentryThrottlingService throttlingService;

    public SentrySyncProcessor(
            Properties sinkProperties,
            SentryClientHolder sentryClientHolder,
            SentryEventConverter eventConverter,
            MetricsCollector metricsCollector,
            Router<Event, SentryDestination> router
    ) {
        this.retryLimit = PropertiesUtil.get(Props.RETRY_LIMIT, sinkProperties).get();
        this.sentryClientHolder = sentryClientHolder;
        this.eventConverter = eventConverter;
        this.metricsCollector = metricsCollector;
        this.router = router;

        long clientsUpdatePeriodMs = PropertiesUtil.get(Props.CLIENTS_UPDATE_PERIOD_MS, sinkProperties).get();
        this.executor = Executors.newSingleThreadScheduledExecutor(
                ThreadFactories.newDaemonNamedThreadFactory("sentry-clients-update"));
        this.executor.scheduleAtFixedRate(
                this.sentryClientHolder::update,
                clientsUpdatePeriodMs,
                clientsUpdatePeriodMs,
                TimeUnit.MILLISECONDS);

        Properties rateLimiterProperties = PropertiesUtil.ofScope(sinkProperties, "throttling.rate");
        this.throttlingService = new SentryThrottlingService(rateLimiterProperties, metricsCollector);
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

        final Optional<Level> level = ContainerUtil.extract(event.getPayload(), LogEventTags.LEVEL_TAG)
                .flatMap(SentryLevelEnumParser::parse);
        if (level.isEmpty() || defaultLevel.compareTo(level.get()) < 0) {
            return false;
        }
        SentryDestination destination = router.route(event).sanitize();
        if (destination.isNowhere()) {
            return false;
        }
        final String prefix = makePrefix(destination.organization(), destination.project());
        boolean processed =
                throttlingService.check(destination.organization(), eventTimestampMs)
                        && tryToSend(event, destination.organization(), destination.project());

        final long processingTimeMs = System.currentTimeMillis() - sendingStart;
        eventProcessingTimerMap
                .computeIfAbsent(prefix, p -> metricsCollector.timer(p + "eventProcessingTimeMs"))
                .update(processingTimeMs, TimeUnit.MILLISECONDS);
        if (processed) {
            processedEventsMeterMap
                    .computeIfAbsent(prefix, p -> metricsCollector.meter(p + "processedEvents"))
                    .mark();
        }

        return processed;
    }

    /**
     * Try to send the event to Sentry.
     * The method executes retry if a retryable error has occurred
     *
     * @param event event to send
     * @param organization Sentry organization where to send event
     * @param sentryProject Sentry project where to send event
     * @return true if event is successfully sent
     * or returns false if a non retryable error occurred
     * @throws BackendServiceFailedException if retryable error occurred on every attempt of retry,
     * error has recommended waiting time
     * or not described exception occurred
     */
    private boolean tryToSend(Event event, String organization, String sentryProject)
            throws BackendServiceFailedException {
        int retryCount = retryLimit;
        do {
            ErrorInfo processErrorInfo;
            Result<Void, ErrorInfo> result = sendToSentry(event, organization, sentryProject);
            if (result.isOk()) {
                return true;
            }
            processErrorInfo = result.getError();
            markError(processErrorInfo, organization, sentryProject);
            if (processErrorInfo.isRetryable() == null) {
                throw new BackendServiceFailedException();
            }
            if (!processErrorInfo.isRetryable()) {
                return false;
            }
            if (processErrorInfo.needDropAfterRetry() && retryCount == 0) {
                return false;
            }
            if (processErrorInfo.needToRemoveClientFromCache()) {
                sentryClientHolder.removeClientFromCache(organization, sentryProject);
            }
        } while (0 < retryCount--);
        throw new BackendServiceFailedException();
    }

    /**
     * Execute following operations: <p>
     * - getting Sentry client from the cache or creating Sentry client in Sentry and in the cache; <p>
     * - converting Hercules event to Sentry event; <p>
     * - sending event to Sentry. <p>
     * The method handle different types of errors when executing this operations
     *
     * @param event event to send
     * @param organization Sentry organization where to send event
     * @param sentryProject Sentry project where to send event
     * @return the {@link Result} object with error information in case of error
     */
    private Result<Void, ErrorInfo> sendToSentry(Event event, String organization, String sentryProject) {

        ErrorInfo processErrorInfo;

        Result<SentryClient, ErrorInfo> sentryClientResult =
                sentryClientHolder.getOrCreateClient(organization, sentryProject);
        if (!sentryClientResult.isOk()) {
            processErrorInfo = sentryClientResult.getError();
            processErrorInfo.setIsRetryableForApiClient();
            return Result.error(processErrorInfo);
        }

        io.sentry.event.Event sentryEvent;
        try {
            sentryEvent = eventConverter.convert(event);
        } catch (Exception e) {
            LOGGER.error("An exception occurred while converting Hercules-event to Sentry-event.", e);
            return Result.error(new ErrorInfo("ConvertingError", false));
        }

        try {
            sentryClientResult.get().sendEvent(sentryEvent);
            return Result.ok();
        } catch (TooManyRequestsException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("TooManyRequestsException. Organization '" + organization + "', project '" + sentryProject +
                        "'. Waiting time: " + e.getRecommendedLockdownTime() + " ms");
            }
            processErrorInfo = new ErrorInfo("TooManyRequestsException", e.getResponseCode());
        } catch (InvalidDsnException e) {
            LOGGER.error("InvalidDsnException", e);
            processErrorInfo = new ErrorInfo("InvalidDSN", false);
        } catch (ConnectionException e) {
            Integer responseCode = e.getResponseCode();
            if (responseCode != null) {
                LOGGER.error("ConnectionException {}", responseCode, e);
                processErrorInfo = new ErrorInfo("ConnectionException", responseCode);
            } else {
                LOGGER.error("ConnectionException", e);
                processErrorInfo = new ErrorInfo("ConnectionException");
            }
        } catch (Exception e) {
            LOGGER.error("Other exception of sending to Sentry", e);
            processErrorInfo = new ErrorInfo("OtherException", e.getMessage());
        }
        processErrorInfo.setIsRetryableForSending();

        return Result.error(processErrorInfo);
    }

    private void markError(ErrorInfo errorInfo, String organization, String sentryProject) {
        String prefix = makePrefix(organization, sentryProject);
        int code = errorInfo.getCode();
        if (code == HttpStatusCodes.TOO_MANY_REQUESTS) {
            throttledBySentryEventsMeterMap
                    .computeIfAbsent(prefix, p -> metricsCollector.meter(p + "throttledBySentryEvents"))
                    .mark();
            return;
        }
        String errorType = errorInfo.getType();
        if (code > 0) {
            errorType += "_" + code;
        }
        errorTypesMeterMap
                .computeIfAbsent(prefix + "errorTypes." + errorType, metricsCollector::meter)
                .mark();
    }

    private String makePrefix(final String organization, final String project) {
        return "byOrgsAndProjects." + organization + "." + project + ".";
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
}
