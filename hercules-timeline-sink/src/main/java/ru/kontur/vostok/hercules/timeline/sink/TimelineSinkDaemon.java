package ru.kontur.vostok.hercules.timeline.sink;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.cassandra.util.Slicer;
import ru.kontur.vostok.hercules.configuration.PropertiesLoader;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.configuration.util.ArgsParser;
import ru.kontur.vostok.hercules.curator.CuratorClient;
import ru.kontur.vostok.hercules.health.CommonMetrics;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.meta.timeline.Timeline;
import ru.kontur.vostok.hercules.meta.timeline.TimelineRepository;
import ru.kontur.vostok.hercules.partitioner.HashPartitioner;
import ru.kontur.vostok.hercules.partitioner.NaiveHasher;
import ru.kontur.vostok.hercules.partitioner.RandomPartitioner;
import ru.kontur.vostok.hercules.partitioner.ShardingKey;
import ru.kontur.vostok.hercules.sink.SinkPool;
import ru.kontur.vostok.hercules.undertow.util.servers.ApplicationStatusHttpServer;
import ru.kontur.vostok.hercules.util.application.ApplicationContextHolder;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class TimelineSinkDaemon {
    private static final Logger LOGGER = LoggerFactory.getLogger(TimelineSinkDaemon.class);

    private CuratorClient curatorClient;
    private ApplicationStatusHttpServer applicationStatusHttpServer;
    private MetricsCollector metricsCollector;
    private TimelineSender sender;
    private ExecutorService executor;
    private SinkPool sinkPool;

    public static void main(String[] args) {
        new TimelineSinkDaemon().run(args);
    }

    public void run(String[] args) {
        long start = System.currentTimeMillis();

        Map<String, String> parameters = ArgsParser.parse(args);

        Properties properties = PropertiesLoader.load(parameters.getOrDefault("application.properties", "file://application.properties"));

        Properties curatorProperties = PropertiesUtil.ofScope(properties, Scopes.CURATOR);
        Properties sinkProperties = PropertiesUtil.ofScope(properties, Scopes.SINK);
        Properties contextProperties = PropertiesUtil.ofScope(properties, Scopes.CONTEXT);
        Properties metricsProperties = PropertiesUtil.ofScope(properties, Scopes.METRICS);
        Properties statusServerProperties = PropertiesUtil.ofScope(properties, Scopes.HTTP_SERVER);

        Properties senderProperties = PropertiesUtil.ofScope(sinkProperties, Scopes.SENDER);

        ApplicationContextHolder.init(getDaemonName(), getDaemonId(), contextProperties);

        //TODO: Validate sinkProperties
        final String timelineName = PropertiesUtil.get(Props.TIMELINE, sinkProperties).get();

        try {
            metricsCollector = new MetricsCollector(metricsProperties);
            metricsCollector.start();
            CommonMetrics.registerCommonMetrics(metricsCollector);

            applicationStatusHttpServer = new ApplicationStatusHttpServer(statusServerProperties);
            applicationStatusHttpServer.start();

            curatorClient = new CuratorClient(curatorProperties);
            curatorClient.start();

            TimelineRepository timelineRepository = new TimelineRepository(curatorClient);

            Optional<Timeline> timelineOptional = timelineRepository.read(timelineName);
            if (!timelineOptional.isPresent()) {
                throw new IllegalArgumentException("Unknown timeline");
            }

            Timeline timeline = timelineOptional.get();

            Slicer slicer =
                    new Slicer(
                            new HashPartitioner(new NaiveHasher()),
                            new RandomPartitioner(),
                            ShardingKey.fromKeyPaths(timeline.getShardingKey()),
                            timeline.getSlices());

            sender = new TimelineSender(timeline, slicer, senderProperties, metricsCollector);
            sender.start();

            int poolSize = PropertiesUtil.get(Props.POOL_SIZE, sinkProperties).get();
            executor = Executors.newFixedThreadPool(poolSize);//TODO: Provide custom ThreadFactory

            sinkPool =
                    new SinkPool(
                            poolSize,
                            () -> new TimelineSink(
                                    executor,
                                    sinkProperties,
                                    sender,
                                    metricsCollector,
                                    timeline));
            sinkPool.start();
        } catch (Throwable t) {
            LOGGER.error("Error on starting timeline sink daemon", t);
            shutdown();
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

        LOGGER.info("Stream Sink Daemon started for {} millis", System.currentTimeMillis() - start);
    }

    public void shutdown() {
        long start = System.currentTimeMillis();
        LOGGER.info("Prepare Timeline Sink Daemon to be shutdown");

        try {
            if (sinkPool != null) {
                sinkPool.stop();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping sink pool", t);
            //TODO: Process error
        }

        try {
            if (executor != null) {
                executor.shutdown();
                executor.awaitTermination(5_000L, TimeUnit.MILLISECONDS);
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping sink thread executor", t);
        }

        try {
            if (sender != null) {
                sender.stop(5_000, TimeUnit.MILLISECONDS);
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping sender", t);
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
            if (Objects.nonNull(applicationStatusHttpServer)) {
                applicationStatusHttpServer.stop();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping status server", t);
            //TODO: Process error
        }

        try {
            if (metricsCollector != null) {
                metricsCollector.stop();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping metrics collector", t);
        }

        LOGGER.info("Finished Timeline Sink Daemon shutdown for {} millis", System.currentTimeMillis() - start);
    }

    protected String getDaemonId() {
        return "sink.timeline";
    }

    protected String getDaemonName() {
        return "Timeline Sink";
    }

    private static class Props {
        static final Parameter<String> TIMELINE =
                Parameter.stringParameter("timeline").
                        required().
                        build();

        static final Parameter<Integer> POOL_SIZE =
                Parameter.integerParameter("poolSize").
                        withDefault(1).
                        build();
    }
}
