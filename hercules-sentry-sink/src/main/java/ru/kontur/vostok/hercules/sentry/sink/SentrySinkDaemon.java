package ru.kontur.vostok.hercules.sentry.sink;

import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.configuration.util.ArgsParser;
import ru.kontur.vostok.hercules.configuration.util.PropertiesReader;
import ru.kontur.vostok.hercules.configuration.util.PropertiesUtil;
import ru.kontur.vostok.hercules.sentry.api.SentryApiClient;
import ru.kontur.vostok.hercules.util.PatternMatcher;
import ru.kontur.vostok.hercules.util.properties.PropertiesExtractor;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class SentrySinkDaemon {
    private static SentrySink sentrySink;

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        Map<String, String> parameters = ArgsParser.parse(args);

        Properties properties = PropertiesReader.read(parameters.getOrDefault("application.properties", "application.properties"));

        Properties streamsProperties = PropertiesUtil.ofScope(properties, Scopes.STREAMS);
        Properties sentryProperties = PropertiesUtil.ofScope(properties, Scopes.SINK);

        //TODO: Validate sinkProperties
        if (!streamsProperties.containsKey("stream.name")) {
            System.out.println("Validation fails (streams.properties): 'stream.name' should be specified");
            return;
        }

        try {
            String streamPattern = PropertiesExtractor.getRequiredProperty(streamsProperties, "stream.name", String.class);
            String sentryUrl = PropertiesExtractor.getRequiredProperty(sentryProperties, "sentry.url", String.class);
            String sentryToken = PropertiesExtractor.getRequiredProperty(sentryProperties, "sentry.token", String.class);

            sentrySink = new SentrySink(streamsProperties, new PatternMatcher(streamPattern), new SentrySyncProcessor(new SentryClientHolder(new SentryApiClient(sentryUrl, sentryToken))));
            sentrySink.start();
        } catch (Throwable e) {
            e.printStackTrace();
            shutdown();
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(SentrySinkDaemon::shutdown));

        System.out.println("Stream Sink Daemon started for " + (System.currentTimeMillis() - start) + " millis");
    }

    public static void shutdown() {
        long start = System.currentTimeMillis();
        System.out.println("Prepare Timeline Sink Daemon to be shutdown");

        try {
            if (sentrySink != null) {
                sentrySink.stop(5_000, TimeUnit.MILLISECONDS);
            }
        } catch (Throwable e) {
            e.printStackTrace();//TODO: Process error
        }

        System.out.println("Finished Timeline Sink Daemon shutdown for " + (System.currentTimeMillis() - start) + " millis");
    }
}
