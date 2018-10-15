package ru.kontur.vostok.hercules.sentry.sink;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.configuration.util.ArgsParser;
import ru.kontur.vostok.hercules.configuration.util.PropertiesReader;
import ru.kontur.vostok.hercules.configuration.util.PropertiesUtil;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.meta.curator.CuratorClient;
import ru.kontur.vostok.hercules.meta.sink.sentry.SentryProjectRepository;
import ru.kontur.vostok.hercules.sentry.api.SentryApiClient;
import ru.kontur.vostok.hercules.util.PatternMatcher;
import ru.kontur.vostok.hercules.util.application.ApplicationContextHolder;
import ru.kontur.vostok.hercules.util.properties.PropertiesExtractor;

import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class SentrySinkDaemon {

    private static final Logger LOGGER = LoggerFactory.getLogger(SentrySinkDaemon.class);

    private static SentrySink sentrySink;
    private static CuratorClient curatorClient;
    private static SentryProjectRegistry sentryProjectRegistry;
    private static MetricsCollector metricsCollector;

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        Map<String, String> parameters = ArgsParser.parse(args);

        Properties properties = PropertiesReader.read(parameters.getOrDefault("application.properties", "application.properties"));

        Properties streamsProperties = PropertiesUtil.ofScope(properties, Scopes.STREAMS);
        Properties sentryProperties = PropertiesUtil.ofScope(properties, Scopes.SINK);
        Properties curatorProperties = PropertiesUtil.ofScope(properties, Scopes.CURATOR);
        Properties metricsProperties = PropertiesUtil.ofScope(properties, Scopes.METRICS);
        Properties contextProperties = PropertiesUtil.ofScope(properties, Scopes.CONTEXT);

        ApplicationContextHolder.init("sink.sentry", contextProperties);

        try {
            String streamPattern = PropertiesExtractor.getRequiredProperty(streamsProperties, "stream.pattern", String.class);
            String sentryUrl = PropertiesExtractor.getRequiredProperty(sentryProperties, "sentry.url", String.class);
            String sentryToken = PropertiesExtractor.getRequiredProperty(sentryProperties, "sentry.token", String.class);

            curatorClient = new CuratorClient(curatorProperties);
            curatorClient.start();

            sentryProjectRegistry = new SentryProjectRegistry(new SentryProjectRepository(curatorClient));
            sentryProjectRegistry.start();

            metricsCollector = new MetricsCollector(metricsProperties);
            metricsCollector.start();

            sentrySink = new SentrySink(
                    streamsProperties,
                    new PatternMatcher(streamPattern),
                    new SentrySyncProcessor(
                            sentryProperties,
                            new SentryClientHolder(
                                    new SentryApiClient(sentryUrl, sentryToken)
                            ),
                            sentryProjectRegistry,
                            metricsCollector
                    )
            );
            sentrySink.start();
        } catch (Throwable e) {
            LOGGER.error("Error on startup", e);
            shutdown();
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(SentrySinkDaemon::shutdown));

        LOGGER.info("Stream Sink Daemon started for {} millis", System.currentTimeMillis() - start);
    }

    public static void shutdown() {
        long start = System.currentTimeMillis();
        LOGGER.info("Prepare Timeline Sink Daemon to be shutdown");

        try {
            if (sentrySink != null) {
                sentrySink.stop(5_000, TimeUnit.MILLISECONDS);
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping sentry sink ", t);
        }
        try {
            if (Objects.nonNull(metricsCollector)) {
                metricsCollector.stop();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping metrics collector", t);
        }
        try {
            if (Objects.nonNull(sentryProjectRegistry)) {
                sentryProjectRegistry.stop();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping sentry project registry", t);
        }
        try {
            if (Objects.nonNull(curatorClient)) {
                curatorClient.stop();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping curator client", t);
        }

        LOGGER.info("Finished Timeline Sink Daemon shutdown for {} millis", System.currentTimeMillis() - start);
    }
}
