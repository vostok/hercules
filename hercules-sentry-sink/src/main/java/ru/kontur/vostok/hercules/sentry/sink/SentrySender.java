package ru.kontur.vostok.hercules.sentry.sink;

import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.kafka.util.processing.BackendServiceFailedException;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.sentry.api.SentryApiClient;
import ru.kontur.vostok.hercules.sink.ProcessorStatus;
import ru.kontur.vostok.hercules.sink.Sender;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

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

        final String sentryUrl = PropertiesUtil.get(Props.SENTRY_URL, senderProperties).get();
        final String sentryToken = PropertiesUtil.get(Props.SENTRY_TOKEN, senderProperties).get();
        sentryApiClient = new SentryApiClient(sentryUrl, sentryToken);
        SentryClientHolder sentryClientHolder = new SentryClientHolder(sentryApiClient);
        this.processor = new SentrySyncProcessor(senderProperties, sentryClientHolder, metricsCollector);
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
        return sentryApiClient.ping().isOk() ? ProcessorStatus.AVAILABLE : ProcessorStatus.UNAVAILABLE;
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
    }
}
