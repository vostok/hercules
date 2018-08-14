package ru.kontur.vostok.hercules.timeline.sink;

import ru.kontur.vostok.hercules.cassandra.util.CassandraConnector;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.configuration.util.ArgsParser;
import ru.kontur.vostok.hercules.configuration.util.PropertiesReader;
import ru.kontur.vostok.hercules.configuration.util.PropertiesUtil;
import ru.kontur.vostok.hercules.meta.curator.CuratorClient;
import ru.kontur.vostok.hercules.meta.timeline.Timeline;
import ru.kontur.vostok.hercules.meta.timeline.TimelineRepository;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class TimelineSinkDaemon {
    private static CuratorClient curatorClient;
    private static CassandraConnector cassandraConnector;
    private static TimelineSink timelineSink;

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        Map<String, String> parameters = ArgsParser.parse(args);

        Properties properties = PropertiesReader.read(parameters.getOrDefault("application.properties", "application.properties"));

        Properties streamsProperties = PropertiesUtil.ofScope(properties, Scopes.STREAMS);
        Properties curatorProperties = PropertiesUtil.ofScope(properties, Scopes.CURATOR);
        Properties sinkProperties = PropertiesUtil.ofScope(properties, Scopes.SINK);
        Properties cassandraProperties = PropertiesUtil.ofScope(properties, Scopes.CASSANDRA);

        //TODO: Validate sinkProperties
        if (!sinkProperties.containsKey("timeline")) {
            System.out.println("Validation fails (sink.properties): 'timeline' should be specified");
            return;
        }

        try {
            curatorClient = new CuratorClient(curatorProperties);
            curatorClient.start();

            cassandraConnector = new CassandraConnector(cassandraProperties);
            cassandraConnector.connect();

            TimelineRepository timelineRepository = new TimelineRepository(curatorClient);

            String timelineName = sinkProperties.getProperty("timeline");
            Optional<Timeline> timelineOptional = timelineRepository.read(timelineName);
            if (!timelineOptional.isPresent()) {
                throw new IllegalArgumentException("Unknown timeline");
            }

            Timeline timeline = timelineOptional.get();
            timelineSink = new TimelineSink(streamsProperties, timeline, cassandraConnector);
            timelineSink.start();
        } catch (Throwable e) {
            e.printStackTrace();
            shutdown();
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(TimelineSinkDaemon::shutdown));

        System.out.println("Stream Sink Daemon started for " + (System.currentTimeMillis() - start) + " millis");
    }

    public static void shutdown() {
        long start = System.currentTimeMillis();
        System.out.println("Prepare Timeline Sink Daemon to be shutdown");

        try {
            if (timelineSink != null) {
                timelineSink.stop(5_000, TimeUnit.MILLISECONDS);
            }
        } catch (Throwable e) {
            e.printStackTrace();//TODO: Process error
        }

        try {
            if (curatorClient != null) {
                curatorClient.stop();
            }
        } catch (Throwable e) {
            e.printStackTrace();//TODO: Process error
        }

        System.out.println("Finished Timeline Sink Daemon shutdown for " + (System.currentTimeMillis() - start) + " millis");
    }
}
