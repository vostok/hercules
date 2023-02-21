package ru.kontur.vostok.hercules.sentry.client.impl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.health.Meter;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.health.MetricsUtil;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.kafka.util.processing.BackendServiceFailedException;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.sentry.client.ErrorInfo;
import ru.kontur.vostok.hercules.sentry.client.SentryClient;
import ru.kontur.vostok.hercules.sentry.client.SentryEventConverter;
import ru.kontur.vostok.hercules.sentry.client.impl.client.RestTemplate;
import ru.kontur.vostok.hercules.sentry.client.impl.client.compression.CompressionStrategies;
import ru.kontur.vostok.hercules.sentry.client.impl.client.compression.Compressor;
import ru.kontur.vostok.hercules.sentry.client.impl.client.compression.GzipCompressorAlgorithm;
import ru.kontur.vostok.hercules.sentry.client.impl.client.retry.RetryStrategies;
import ru.kontur.vostok.hercules.sentry.client.impl.client.v7.StoreEventClient;
import ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model.Dsn;
import ru.kontur.vostok.hercules.sentry.client.impl.exceptions.SentryApiException;
import ru.kontur.vostok.hercules.util.functional.Result;
import ru.kontur.vostok.hercules.util.routing.SentryDestination;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.concurrent.ConcurrentHashMap;

public class SentryClientImpl implements SentryClient {

    private final StoreEventClient storeEventClient;
    private final SentryEventConverter eventConverter;
    private static final ConcurrentHashMap<String, Meter> throttledBySentryEventsMeterMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Meter> errorTypesMeterMap = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, Meter> cannotCreateOrganizationMetermap = new ConcurrentHashMap<>();

    private final SentryConnectorHolderImpl sentryConnectorHolder;

    private final MetricsCollector metricsCollector;

    private final int retryLimit;
    private static final Logger LOGGER = LoggerFactory.getLogger(SentryClientImpl.class);

    public SentryClientImpl(int retryLimit,
                            MetricsCollector metricsCollector,
                            SentryConnectorHolderImpl sentryConnectorHolder,
                            String herculesVersion,
                            String sentryUrl) {
        this.metricsCollector = metricsCollector;
        this.retryLimit = retryLimit;
        ObjectMapper objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .registerModule(new JavaTimeModule())
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.storeEventClient = StoreEventClient.builder()
                .withBaseUri(URI.create(sentryUrl))
                .withCompressor(new Compressor(CompressionStrategies.always(), new GzipCompressorAlgorithm()))
                .withObjectMapper(objectMapper)
                .withRestTemplate(RestTemplate.builder()
                        .withClient(httpClient)
                        .withRetryStrategy(RetryStrategies.onTemporaryErrors(retryLimit))
                        .build()
                )
                .build();
        this.eventConverter = new SentryEventConverterImpl(herculesVersion);
        this.sentryConnectorHolder = sentryConnectorHolder;
    }

    @Override
    public boolean tryToSend(Event event, SentryDestination destination) throws BackendServiceFailedException {
        Result<Void, ErrorInfo> result = sendToSentry(event, destination);
        if (result.isOk()) {
            return true;
        }
        ErrorInfo processErrorInfo = result.getError();
        markError(processErrorInfo, destination);
        if (Boolean.TRUE.equals(processErrorInfo.isRetryable())) {
            throw new BackendServiceFailedException(new Exception(processErrorInfo.toString()));
        }
        return false;
    }

    private Result<Void, ErrorInfo> sendToSentry(Event event, SentryDestination destination) {

        SentryEventImpl sentryEvent;
        try {
            sentryEvent = (SentryEventImpl) eventConverter.convert(event);
        } catch (Exception e) {
            LOGGER.error("An exception occurred while converting Hercules-event to Sentry-event.", e);
            return Result.error(new ErrorInfo("Converting error", false));
        }
        Result<Dsn, ErrorInfo> dsnResult;
        int retryCount = retryLimit;
        do {
            dsnResult = sentryConnectorHolder.getOrCreateConnector(destination);
            if (dsnResult.get() == Dsn.unavailable()) {
                SentryApiException e = new SentryApiException("Organization %s not found in Sentry.", destination.organization(), "");
                e.setCode(HttpStatusCodes.ORGANIZATION_NOT_FOUND);
                return Result.error(new ErrorInfo(e.getMessage(), e.getCode(), false));
            }
            if (dsnResult.isOk()) {
                break;
            }
            if (!dsnResult.isOk()) {
                SentryApiException e = new SentryApiException("Cannot create dsn for organization %s, project %s.", destination.organization(), destination.project());
                ErrorInfo createDsnResult = new ErrorInfo(e.getMessage(), e.getCode());
                createDsnResult.setIsRetryableForApiClient();
                if (!createDsnResult.isRetryable()) {
                    return Result.error(createDsnResult);
                }
            }
        } while (0 < retryCount--);

        try {
            int sendResult = storeEventClient.send(dsnResult.get(), sentryEvent.getSentryEvent());
            if (sendResult > 0) {
                SentryApiException e = new SentryApiException(
                        "Cannot send to Sentry for organization %s, project %s. Response code: %s",
                        destination.organization(),
                        destination.project(),
                        String.valueOf(sendResult));
                ErrorInfo sendEventResult = new ErrorInfo(e.getMessage(), sendResult);
                sendEventResult.setIsRetryableForSending();
                return Result.error(sendEventResult);
            }
            return Result.ok();
        } catch (Exception e) {
            LOGGER.error("Non retryable error occurred: {}", e.getMessage());
            return Result.error(new ErrorInfo(e.getMessage(), false));
        }
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
        if (code == HttpStatusCodes.ORGANIZATION_NOT_FOUND) {
            cannotCreateOrganizationMetermap
                    .computeIfAbsent(prefix + ".Organization not found", metricsCollector::meter)
                    .mark();
        }
    }

    private String getMetricsPrefix(SentryDestination destination) {
        return MetricsUtil.toMetricPath("byOrgsAndProjects", destination.organization(), destination.project());
    }
}
