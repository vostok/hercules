package ru.kontur.vostok.hercules.sentry.sink;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.configuration.util.PropertiesUtil;
import ru.kontur.vostok.hercules.curator.CuratorClient;
import ru.kontur.vostok.hercules.kafka.util.processing.ServicePinger;
import ru.kontur.vostok.hercules.kafka.util.processing.single.AbstractSingleSinkDaemon;
import ru.kontur.vostok.hercules.kafka.util.processing.single.SingleSender;
import ru.kontur.vostok.hercules.meta.sink.sentry.SentryProjectRepository;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.sentry.api.SentryApiClient;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;

import java.util.Objects;
import java.util.Properties;
import java.util.UUID;

/**
 * @author Gregory Koshelev
 */
public class SentrySinkDaemon extends AbstractSingleSinkDaemon {

    private static class Props {
        static final PropertyDescription<String> SENTRY_URL = PropertyDescriptions
                .stringProperty("sentry.url")
                .build();

        static final PropertyDescription<String> SENTRY_TOKEN = PropertyDescriptions
                .stringProperty("sentry.token")
                .build();
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(SentrySinkDaemon.class);

    private CuratorClient curatorClient;

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

        return new SentrySyncProcessor(
                sinkProperties,
                new SentryClientHolder(sinkProperties,
                        new SentryApiClient(sentryUrl, sentryToken))
        );
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

    @Override
    protected void initSink(Properties properties) {
        super.initSink(properties);

        Properties curatorProperties = PropertiesUtil.ofScope(properties, Scopes.CURATOR);

        curatorClient = new CuratorClient(curatorProperties);
        curatorClient.start();
    }

    @Override
    protected void shutdownSink() {
        super.shutdownSink();

        try {
            if (Objects.nonNull(curatorClient)) {
                curatorClient.stop();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping curator client", t);
        }
    }
}
