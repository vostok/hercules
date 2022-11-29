package ru.kontur.vostok.hercules.sentry.client.impl.v9;

import io.sentry.connection.ConnectionException;
import io.sentry.connection.TooManyRequestsException;
import io.sentry.dsn.InvalidDsnException;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.health.Meter;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.health.MetricsUtil;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.kafka.util.processing.BackendServiceFailedException;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.sentry.client.impl.v9.connector.SentryConnectorImplV9;
import ru.kontur.vostok.hercules.util.routing.SentryDestination;
import ru.kontur.vostok.hercules.sentry.client.ErrorInfo;
import ru.kontur.vostok.hercules.sentry.client.SentryClient;
import ru.kontur.vostok.hercules.sentry.client.SentryEventConverter;
import ru.kontur.vostok.hercules.sentry.client.impl.v9.connector.SentryConnectorHolderImplV9;
import ru.kontur.vostok.hercules.util.functional.Result;

/**
 * @author Tatyana Tokmyanina
 */
public class SentryClientImplV9 implements SentryClient {
    private final int retryLimit;
    private final MetricsCollector metricsCollector;
    private final SentryConnectorHolderImplV9 sentryConnectorHolderImplV9;
    private final SentryEventConverter eventConverter;

    private static final ConcurrentHashMap<String, Meter> throttledBySentryEventsMeterMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Meter> errorTypesMeterMap = new ConcurrentHashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(SentryClientImplV9.class);

    public SentryClientImplV9(int retryLimit,
            MetricsCollector metricsCollector,
            SentryConnectorHolderImplV9 sentryConnectorHolderImplV9,
            String herculesVersion
            ) {
        this.retryLimit = retryLimit;
        this.metricsCollector = metricsCollector;
        this.sentryConnectorHolderImplV9 = sentryConnectorHolderImplV9;
        this.eventConverter = new SentryEventConverterImplV9(herculesVersion);
    }

    /**
     * Try to send the event to Sentry.
     * The method executes retry if a retryable error has occurred
     *
     * @param event event to send
     * @param destination Sentry destination where to send event (contains organization and
     *                      project name)
     * @return true if event is successfully sent
     * or returns false if a non retryable error occurred
     * @throws BackendServiceFailedException if retryable error occurred on every attempt of retry,
     * error has recommended waiting time
     * or not described exception occurred
     */
    @Override
    public boolean tryToSend(Event event, SentryDestination destination)
            throws BackendServiceFailedException {
        int retryCount = retryLimit;
        do {
            ErrorInfo processErrorInfo;
            Result<Void, ErrorInfo> result = sendToSentry(event, destination);
            if (result.isOk()) {
                return true;
            }
            processErrorInfo = result.getError();
            markError(processErrorInfo, destination);
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
                sentryConnectorHolderImplV9.removeClientFromCache(destination);
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
     * @param destination Sentry destination where to send event (contains organization and project name)
     * @return the {@link Result} object with error information in case of error
     */
    private Result<Void, ErrorInfo> sendToSentry(Event event, SentryDestination destination) {

        ErrorInfo processErrorInfo;

        Result<SentryConnectorImplV9, ErrorInfo> sentryConnectorResult =
                sentryConnectorHolderImplV9.getOrCreateConnector(destination.organization(),
                        destination.project());
        if (!sentryConnectorResult.isOk()) {
            processErrorInfo = sentryConnectorResult.getError();
            processErrorInfo.setIsRetryableForApiClient();
            return Result.error(processErrorInfo);
        }

        SentryEventImplV9 sentryEvent;
        try {
            sentryEvent = (SentryEventImplV9) eventConverter.convert(event);
        } catch (Exception e) {
            LOGGER.error("An exception occurred while converting Hercules-event to Sentry-event.", e);
            return Result.error(new ErrorInfo("ConvertingError", false));
        }

        try {
            sentryConnectorResult.get().getSentryClient().sendEvent(sentryEvent.getSentryEvent());
            return Result.ok();
        } catch (TooManyRequestsException e) {
            LOGGER.debug("TooManyRequestsException. Organization '" + destination.organization() +
                        "', project '" + destination.project() +
                        "'. Waiting time: " + e.getRecommendedLockdownTime() + " ms");
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

    protected void markError(ErrorInfo errorInfo, SentryDestination destination) {
        String prefix = getMetricsPrefix(destination);
        int code = errorInfo.getCode();
        if (code == HttpStatusCodes.TOO_MANY_REQUESTS) {
            throttledBySentryEventsMeterMap
                    .computeIfAbsent(prefix, p -> this.metricsCollector.meter(p +  ".throttledBySentryEvents"))
                    .mark();
            return;
        }
        String errorType = errorInfo.getType();
        if (code > 0) {
            errorType += "_" + code;
        }
        errorTypesMeterMap
                .computeIfAbsent(prefix + ".errorTypes." + errorType, metricsCollector::meter)
                .mark();
    }

    private String getMetricsPrefix(SentryDestination destination) {
        return MetricsUtil.toMetricPath("byOrgsAndProjects", destination.organization(), destination.project());
    }
}
