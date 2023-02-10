package ru.kontur.vostok.hercules.sentry.sink;

import ru.kontur.vostok.hercules.application.Application;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.kafka.util.processing.BackendServiceFailedException;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.routing.Router;
import ru.kontur.vostok.hercules.sentry.client.SentryConnectorHolder;
import ru.kontur.vostok.hercules.sentry.client.impl.SentryConnectorHolderImpl;
import ru.kontur.vostok.hercules.sentry.client.impl.client.v7.DsnFetcherClient;
import ru.kontur.vostok.hercules.sentry.client.impl.client.v7.DsnFetcherClient.DsnFetcherClientBuilder;
import ru.kontur.vostok.hercules.sentry.client.impl.v9.connector.SentryConnectorHolderImplV9;
import ru.kontur.vostok.hercules.sink.parallel.sender.NoPrepareParallelSender;
import ru.kontur.vostok.hercules.util.routing.SentryDestination;
import ru.kontur.vostok.hercules.sentry.client.api.SentryApiClient;
import ru.kontur.vostok.hercules.sink.ProcessorStatus;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import ru.kontur.vostok.hercules.util.validation.LongValidators;

/**
 * SentrySender sends events to Sentry
 *
 * @author Petr Demenev
 */
public class SentrySender extends NoPrepareParallelSender {

    private final SentrySyncProcessor processor;
    private final SentryApiClient sentryApiClient;

    /**
     * Sentry Sender
     *
     * @param senderProperties sender's properties.
     * @param metricsCollector metrics collector
     * @param router           makes decisions about log events' destinations
     */
    public SentrySender(
            Properties senderProperties,
            MetricsCollector metricsCollector,
            Router<Event, SentryDestination> router
    ) {
        super(senderProperties, metricsCollector);

        final String sentryUrl = PropertiesUtil.get(Props.SENTRY_URL, senderProperties).get();
        final String sentryToken = PropertiesUtil.get(Props.SENTRY_TOKEN, senderProperties).get();
        sentryApiClient = new SentryApiClient(sentryUrl, sentryToken);
        boolean useOldVersion = PropertiesUtil.get(Props.USE_OLD_VERSION, senderProperties).get();
        String herculesVersion = Application.context().getVersion();
        SentryConnectorHolder sentryConnectorHolder;
        if (useOldVersion) {
            sentryConnectorHolder = new SentryConnectorHolderImplV9(
                    sentryApiClient,
                    senderProperties);
        } else {
            long cacheTtlMs = PropertiesUtil.get(Props.CACHE_TTL_MS, senderProperties).get();
            DsnFetcherClient dsnFetcherClient = new DsnFetcherClientBuilder()
                    .withBaseUri(sentryUrl)
                    .withAuthToken(sentryToken)
                    .build();
            sentryConnectorHolder = new SentryConnectorHolderImpl(
                    sentryApiClient,
                    cacheTtlMs,
                    dsnFetcherClient);
        }
        this.processor = new SentrySyncProcessor(
                senderProperties,
                sentryConnectorHolder,
                metricsCollector,
                router,
                sentryUrl,
                herculesVersion);
    }

    @Override
    protected int send(List<Event> events) throws BackendServiceFailedException {
        int rejectedEvents = 0;
        for (Event event : events) {
            boolean processed = processor.process(event);
            if (!processed) {
                rejectedEvents++;
            }
        }
        return events.size() - rejectedEvents;
    }

    @Override
    public ProcessorStatus ping() {
        return sentryApiClient.ping().isOk() ? ProcessorStatus.AVAILABLE
                : ProcessorStatus.UNAVAILABLE;
    }

    private static class Props {
        static final Parameter<String> SENTRY_URL =
                Parameter.stringParameter("sentry.url").
                        required().
                        build();

        static final Parameter<String> SENTRY_TOKEN =
                Parameter.stringParameter("sentry.token").
                        required().
                        build();

        static final Parameter<Boolean> USE_OLD_VERSION =
                Parameter.booleanParameter("sentry.use.old.version").
                        withDefault(true).
                        build();

        static final Parameter<Long> CACHE_TTL_MS =
                Parameter.longParameter("sentry.cache.ttl.ms").
                        withDefault(600_000L).
                        withValidator(LongValidators.nonNegative()).
                        build();
    }

    @Override
    public boolean stop(long timeout, TimeUnit unit) {
        try {
            return super.stop(timeout, unit);
        } finally {
            this.processor.stop();
        }
    }
}
