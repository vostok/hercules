package ru.kontur.vostok.hercules.sentry.sink;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import io.sentry.SentryClient;
import io.sentry.connection.ConnectionException;
import io.sentry.connection.LockdownManager;
import io.sentry.connection.LockedDownException;
import io.sentry.dsn.InvalidDsnException;
import io.sentry.event.Event.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.kafka.util.processing.BackendServiceFailedException;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.sentry.sink.converters.SentryEventConverter;
import ru.kontur.vostok.hercules.sentry.sink.converters.SentryLevelEnumParser;
import ru.kontur.vostok.hercules.tags.CommonTags;
import ru.kontur.vostok.hercules.tags.LogEventTags;
import ru.kontur.vostok.hercules.util.functional.Result;
import ru.kontur.vostok.hercules.util.metrics.GraphiteMetricsUtil;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;

import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * @author Gregory Koshelev
 */
public class SentrySyncProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SentrySyncProcessor.class);

    private final Level requiredLevel;
    private final int retryLimit;
    private final SentryClientHolder sentryClientHolder;
    private final MetricsCollector metricsCollector;
    private final ConcurrentHashMap<String, Meter> errorTypesMeterMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Timer> eventProcessingTimerMap = new ConcurrentHashMap<>();

    public SentrySyncProcessor(
            Properties sinkProperties,
            SentryClientHolder sentryClientHolder,
            MetricsCollector metricsCollector
    ) {
        this.requiredLevel = Props.REQUIRED_LEVEL.extract(sinkProperties);
        this.retryLimit = Props.RETRY_LIMIT.extract(sinkProperties);
        this.sentryClientHolder = sentryClientHolder;
        this.sentryClientHolder.update();
        this.metricsCollector = metricsCollector;
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

        final Optional<Level> level = ContainerUtil.extract(event.getPayload(), LogEventTags.LEVEL_TAG)
                .flatMap(SentryLevelEnumParser::parse);
        if (!level.isPresent() || requiredLevel.compareTo(level.get()) < 0) {
            return false;
        }

        final Optional<Container> properties = ContainerUtil.extract(event.getPayload(), CommonTags.PROPERTIES_TAG);
        if (!properties.isPresent()) {
            LOGGER.warn("Missing required tag '{}'", CommonTags.PROPERTIES_TAG.getName());
            return false;
        }

        Optional<String> organizationName = ContainerUtil.extract(properties.get(), CommonTags.PROJECT_TAG);
        if (!organizationName.isPresent()) {
            LOGGER.warn("Missing required tag '{}'", CommonTags.PROJECT_TAG.getName());
            return false;
        }
        String organization = sanitizeName(organizationName.get());

        Optional<String> sentryProjectName = ContainerUtil.extract(properties.get(), CommonTags.APPLICATION_TAG);
        String sentryProject = sentryProjectName.map(this::sanitizeName).orElse(organization);

        boolean processed = tryToSend(event, organization, sentryProject);

        final long processingTimeMs = System.currentTimeMillis() - sendingStart;
        final String prefix = makePrefix(organization, sentryProject);
        eventProcessingTimerMap.computeIfAbsent(prefix, p -> metricsCollector.timer(p + "eventProcessingTimeMs"))
                .update(processingTimeMs, TimeUnit.MILLISECONDS);

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

            String type = processErrorInfo.getMessage();
            errorTypesMeterMap.computeIfAbsent(
                    type == null ? "null" : type,
                    t -> createMeter(t, organization, sentryProject)
            ).mark();

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
            if (processErrorInfo.getWaitingTimeMs() > 0) {
                throw new BackendServiceFailedException();
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
            LOGGER.error(String.format("Cannot get client for Sentry organization/project '%s/%s'",
                    organization, sentryProject));
            processErrorInfo = sentryClientResult.getError();
            processErrorInfo.setIsRetryableForApiClient();
            return Result.error(processErrorInfo);
        }

        io.sentry.event.Event sentryEvent;
        try {
            sentryEvent = SentryEventConverter.convert(event);
        } catch (Exception e) {
            LOGGER.error("An exception occurred while converting Hercules-event to Sentry-event.", e);
            return Result.error(new ErrorInfo("Converting error", false));
        }

        try {
            sentryClientResult.get().sendEvent(sentryEvent);
            return Result.ok();
        } catch (InvalidDsnException e) {
            LOGGER.error("InvalidDsnException: " + e.getMessage());
            processErrorInfo = new ErrorInfo("Invalid DSN", false);
        } catch (LockedDownException e) {
            LOGGER.error("LockedDownException: a temporary lockdown is switched on");
            processErrorInfo = new ErrorInfo(
                    "LockedDown", true, LockdownManager.DEFAULT_BASE_LOCKDOWN_TIME);
        } catch (ConnectionException e) {
            Integer responseCode = e.getResponseCode();
            String message = e.getMessage();
            if (responseCode != null) {
                LOGGER.error(String.format("ConnectionException: %d %s", responseCode, message));
                if (e.getRecommendedLockdownTime() != null) {
                    processErrorInfo = new ErrorInfo(message, responseCode, e.getRecommendedLockdownTime());
                } else {
                    processErrorInfo = new ErrorInfo(message, responseCode);
                }
            } else {
                LOGGER.error(String.format("ConnectionException: %s", message));
                processErrorInfo = new ErrorInfo(message);
            }
        } catch (Exception e) {
            processErrorInfo = new ErrorInfo(e.getMessage());
        }
        processErrorInfo.setIsRetryableForSending();

        return Result.error(processErrorInfo);
    }

    private String sanitizeName(String name) {
        Pattern pattern = Pattern.compile("[^-_a-z0-9]");
        return pattern.matcher(name.toLowerCase()).replaceAll("_");
    }

    private String makePrefix(final String organization, final String project) {
        return "byOrgsAndProjects." + organization + "." + project + ".";
    }

    private Meter createMeter(String errorType, final String organization, final String project) {
        String prefix = makePrefix(organization, project);
        return metricsCollector.meter(prefix + "errorTypes." + GraphiteMetricsUtil.sanitizeMetricName(errorType));
    }

    private static class Props {
        static final PropertyDescription<Level> REQUIRED_LEVEL = PropertyDescriptions
                .propertyOfType(Level.class, "sentry.level")
                .withParser(SentryLevelEnumParser::parseAsResult)
                .withDefaultValue(Level.WARNING)
                .build();

        static final PropertyDescription<Integer> RETRY_LIMIT = PropertyDescriptions
                .integerProperty("sentry.retryLimit")
                .withDefaultValue(3)
                .build();
    }
}
