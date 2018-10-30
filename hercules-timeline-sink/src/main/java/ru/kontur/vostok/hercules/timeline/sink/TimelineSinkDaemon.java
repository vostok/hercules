package ru.kontur.vostok.hercules.timeline.sink;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.cassandra.util.CassandraConnector;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.configuration.util.ArgsParser;
import ru.kontur.vostok.hercules.configuration.util.PropertiesReader;
import ru.kontur.vostok.hercules.configuration.util.PropertiesUtil;
import ru.kontur.vostok.hercules.meta.curator.CuratorClient;
import ru.kontur.vostok.hercules.meta.timeline.Timeline;
import ru.kontur.vostok.hercules.meta.timeline.TimelineRepository;
import ru.kontur.vostok.hercules.undertow.util.servers.MinimalStatusServer;
import ru.kontur.vostok.hercules.util.application.ApplicationContextHolder;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class TimelineSinkDaemon {

    private static class Props {
        static final PropertyDescription<String> TIMELINE = PropertyDescriptions
                .stringProperty("timeline")
                .build();
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(TimelineSinkDaemon.class);

    private static CuratorClient curatorClient;
    private static CassandraConnector cassandraConnector;
    private static TimelineSink timelineSink;
    private static MinimalStatusServer minimalStatusServer;

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        Map<String, String> parameters = ArgsParser.parse(args);

        Properties properties = PropertiesReader.read(parameters.getOrDefault("application.properties", "application.properties"));

        Properties streamsProperties = PropertiesUtil.ofScope(properties, Scopes.STREAMS);
        Properties curatorProperties = PropertiesUtil.ofScope(properties, Scopes.CURATOR);
        Properties sinkProperties = PropertiesUtil.ofScope(properties, Scopes.SINK);
        Properties cassandraProperties = PropertiesUtil.ofScope(properties, Scopes.CASSANDRA);
        Properties contextProperties = PropertiesUtil.ofScope(properties, Scopes.CONTEXT);
        Properties statusServerProperties = PropertiesUtil.ofScope(properties, Scopes.HTTP_SERVER);

        ApplicationContextHolder.init("Hercules timeline sink", "sink.timeline", contextProperties);

        //TODO: Validate sinkProperties
        final String timelineName = Props.TIMELINE.extract(sinkProperties);

        try {
            minimalStatusServer = new MinimalStatusServer(statusServerProperties);
            minimalStatusServer.start();

            curatorClient = new CuratorClient(curatorProperties);
            curatorClient.start();

            cassandraConnector = new CassandraConnector(cassandraProperties);
            cassandraConnector.connect();

            TimelineRepository timelineRepository = new TimelineRepository(curatorClient);

            Optional<Timeline> timelineOptional = timelineRepository.read(timelineName);
            if (!timelineOptional.isPresent()) {
                throw new IllegalArgumentException("Unknown timeline");
            }

            Timeline timeline = timelineOptional.get();
            timelineSink = new TimelineSink(streamsProperties, timeline, cassandraConnector);
            timelineSink.start();
        } catch (Throwable t) {
            LOGGER.error("Error on starting timeline sink daemon", t);
            shutdown();
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(TimelineSinkDaemon::shutdown));

        LOGGER.info("Stream Sink Daemon started for {} millis", System.currentTimeMillis() - start);
    }

    public static void shutdown() {
        long start = System.currentTimeMillis();
        LOGGER.info("Prepare Timeline Sink Daemon to be shutdown");

        try {
            if (timelineSink != null) {
                timelineSink.stop(5_000, TimeUnit.MILLISECONDS);
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping timeline sink", t);
            //TODO: Process error
        }

        try {
            if (curatorClient != null) {
                curatorClient.stop();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping curator client", t);
            //TODO: Process error
        }

        try {
            if (Objects.nonNull(minimalStatusServer)) {
                minimalStatusServer.stop();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping status server", t);
            //TODO: Process error
        }

        LOGGER.info("Finished Timeline Sink Daemon shutdown for {} millis", System.currentTimeMillis() - start);
    }
}
