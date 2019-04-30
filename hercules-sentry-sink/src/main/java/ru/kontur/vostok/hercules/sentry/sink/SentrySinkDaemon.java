package ru.kontur.vostok.hercules.sentry.sink;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.kafka.util.processing.ServicePinger;
import ru.kontur.vostok.hercules.kafka.util.processing.single.AbstractSingleSinkDaemon;
import ru.kontur.vostok.hercules.kafka.util.processing.single.SingleSender;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.sentry.api.SentryApiClient;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;

import java.util.Properties;
import java.util.UUID;

/**
 * @author Gregory Koshelev
 */
public class SentrySinkDaemon extends AbstractSingleSinkDaemon {

    private static final Logger LOGGER = LoggerFactory.getLogger(SentrySinkDaemon.class);

    /**
     * Main starting point
     */
    public static void main(String[] args) {
        new SentrySinkDaemon().run(args);
    }

    @Override
    protected SingleSender<UUID, Event> createSender(Properties sinkProperties) {
        final String sentryUrl = Props.SENTRY_URL.extract(sinkProperties);
        final String sentryToken = Props.SENTRY_TOKEN.extract(sinkProperties);

        SentryApiClient sentryApiClient = new SentryApiClient(sentryUrl, sentryToken);
        SentryClientHolder sentryClientHolder = new SentryClientHolder(sentryApiClient);
        sentryClientHolder.update();
        return new SentrySyncProcessor(sinkProperties,sentryClientHolder);
    }

    @Override
    protected ServicePinger createPinger(Properties sinkProperties) {
        final String sentryUrl = Props.SENTRY_URL.extract(sinkProperties);
        final String sentryToken = Props.SENTRY_TOKEN.extract(sinkProperties);

        SentryApiClient sentryApiClient = new SentryApiClient(sentryUrl, sentryToken);

        return () -> sentryApiClient.ping().isOk();
    }

    @Override
    protected String getDaemonName() {
        return "Hercules sentry sink";
    }

    @Override
    protected String getDaemonId() {
        return "sink.sentry";
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
