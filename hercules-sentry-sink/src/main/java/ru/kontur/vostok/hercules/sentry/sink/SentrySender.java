package ru.kontur.vostok.hercules.sentry.sink;

import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.kafka.util.processing.BackendServiceFailedException;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.sentry.api.SentryApiClient;
import ru.kontur.vostok.hercules.sink.Sender;
import ru.kontur.vostok.hercules.sink.SenderStatus;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;

import java.util.List;
import java.util.Properties;

/**
 * SentrySender sends events to Sentry
 *
 * @author Petr Demenev
 */
public class SentrySender extends Sender {

    private final SentrySyncProcessor processor;
    private final SentryApiClient sentryApiClient;

    /**
     * Sentry Sender
     *
     * @param senderProperties sender's properties.
     * @param metricsCollector metrics collector
     */
    public SentrySender(Properties senderProperties, MetricsCollector metricsCollector) {
        super(senderProperties, metricsCollector);

        final String sentryUrl = Props.SENTRY_URL.extract(senderProperties);
        final String sentryToken = Props.SENTRY_TOKEN.extract(senderProperties);
        sentryApiClient = new SentryApiClient(sentryUrl, sentryToken);
        SentryClientHolder sentryClientHolder = new SentryClientHolder(sentryApiClient);
        this.processor = new SentrySyncProcessor(senderProperties, sentryClientHolder);
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
    public SenderStatus ping() {
        return sentryApiClient.ping().isOk() ? SenderStatus.AVAILABLE : SenderStatus.UNAVAILABLE;
    }

    private static class Props {
        static final PropertyDescription<String> SENTRY_URL = PropertyDescriptions
                .stringProperty("sentry.url")
                .build();

        static final PropertyDescription<String> SENTRY_TOKEN = PropertyDescriptions
                .stringProperty("sentry.token")
                .build();
    }
}
