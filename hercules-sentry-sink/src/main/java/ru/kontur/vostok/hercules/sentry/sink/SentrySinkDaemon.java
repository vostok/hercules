package ru.kontur.vostok.hercules.sentry.sink;

import ru.kontur.vostok.hercules.util.PatternMatcher;
import ru.kontur.vostok.hercules.sentry.api.SentryApiClient;
import ru.kontur.vostok.hercules.util.args.ArgsParser;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

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

        Properties streamsProperties = PropertiesUtil.readProperties(parameters.getOrDefault("streams.properties", "streams.properties"));
        Properties curatorProperties = PropertiesUtil.readProperties(parameters.getOrDefault("curator.properties", "curator.properties"));
        Properties sentryProperties = PropertiesUtil.readProperties(parameters.getOrDefault("sentry.properties", "sentry.properties"));

        //TODO: Validate sinkProperties
        if (!streamsProperties.containsKey("stream.name")) {
            System.out.println("Validation fails (streams.properties): 'stream.name' should be specified");
            return;
        }

        try {
            String streamPattern = PropertiesUtil.getRequiredProperty(streamsProperties, "stream.name", String.class);
            String sentryUrl = PropertiesUtil.getRequiredProperty(sentryProperties, "sentry.url", String.class);
            String sentryToken = PropertiesUtil.getRequiredProperty(sentryProperties, "sentry.token", String.class);

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
