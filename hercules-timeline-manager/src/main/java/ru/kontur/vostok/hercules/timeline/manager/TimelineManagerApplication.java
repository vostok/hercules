package ru.kontur.vostok.hercules.timeline.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.application.Application;
import ru.kontur.vostok.hercules.cassandra.util.CassandraConnector;
import ru.kontur.vostok.hercules.configuration.PropertiesLoader;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.configuration.util.ArgsParser;
import ru.kontur.vostok.hercules.curator.CuratorClient;
import ru.kontur.vostok.hercules.health.CommonMetrics;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.meta.task.timeline.TimelineTaskRepository;
import ru.kontur.vostok.hercules.meta.timeline.TimelineRepository;
import ru.kontur.vostok.hercules.undertow.util.servers.DaemonHttpServer;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class TimelineManagerApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimelineManagerApplication.class);

    private static CassandraConnector cassandraConnector;
    private static CuratorClient curatorClient;
    private static TimelineTaskExecutor timelineTaskExecutor;
    private static DaemonHttpServer daemonHttpServer;
    private static MetricsCollector metricsCollector;

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        try {
            Application.run("Hercules Timeline Manager", "timeline-manager", args);
            Map<String, String> parameters = ArgsParser.parse(args);

            Properties properties = PropertiesLoader.load(parameters.getOrDefault("application.properties", "file://application.properties"));
            Properties cassandraProperties = PropertiesUtil.ofScope(properties, Scopes.CASSANDRA);
            Properties curatorProperties = PropertiesUtil.ofScope(properties, Scopes.CURATOR);
            Properties statusServerProperties = PropertiesUtil.ofScope(properties, Scopes.HTTP_SERVER);
            Properties metricsProperties = PropertiesUtil.ofScope(properties, Scopes.METRICS);

            cassandraConnector = new CassandraConnector(cassandraProperties);

            CassandraManager cassandraManager = new CassandraManager(cassandraConnector);

            curatorClient = new CuratorClient(curatorProperties);
            curatorClient.start();

            metricsCollector = new MetricsCollector(metricsProperties);
            metricsCollector.start();
            CommonMetrics.registerCommonMetrics(metricsCollector);

            TimelineTaskRepository timelineTaskRepository = new TimelineTaskRepository(curatorClient);
            TimelineRepository timelineRepository = new TimelineRepository(curatorClient);

            timelineTaskExecutor = new TimelineTaskExecutor(
                    timelineTaskRepository,
                    5_000,
                    cassandraManager,
                    timelineRepository,
                    metricsCollector);
            timelineTaskExecutor.start();

            daemonHttpServer = new DaemonHttpServer(statusServerProperties, metricsCollector);
            daemonHttpServer.start();
        } catch (Throwable t) {
            LOGGER.error("Error on starting Timeline Manager", t);
            shutdown();
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(TimelineManagerApplication::shutdown));

        LOGGER.info("Timeline Manager started for {} millis", System.currentTimeMillis() - start);
    }

    private static void shutdown() {
        long start = System.currentTimeMillis();
        LOGGER.info("Started Timeline Manager shutdown");

        try {
            if (daemonHttpServer != null) {
                daemonHttpServer.stop(5_000, TimeUnit.MILLISECONDS);
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping http gserver", t);
            //TODO: Process error
        }

        try {
            if (timelineTaskExecutor != null) {
                timelineTaskExecutor.stop(5_000, TimeUnit.MILLISECONDS);
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping task executor", t);
            //TODO: Process error
        }

        try {
            if (metricsCollector != null) {
                metricsCollector.stop();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping metrics collector", t);
        }

        try {
            if (curatorClient != null) {
                curatorClient.stop();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping curator", t);
            //TODO: Process error
        }

        try {
            if (cassandraConnector != null) {
                cassandraConnector.close();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping cassandra connector", t);
            //TODO: Process error
        }

        LOGGER.info("Finished Timeline Manager shutdown for {} millis", System.currentTimeMillis() - start);
    }
}
