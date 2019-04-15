package ru.kontur.vostok.hercules.timeline.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.cassandra.util.CassandraConnector;
import ru.kontur.vostok.hercules.configuration.PropertiesLoader;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.configuration.util.ArgsParser;
import ru.kontur.vostok.hercules.configuration.util.PropertiesUtil;
import ru.kontur.vostok.hercules.curator.CuratorClient;
import ru.kontur.vostok.hercules.health.CommonMetrics;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.meta.timeline.TimelineRepository;
import ru.kontur.vostok.hercules.util.application.ApplicationContextHolder;

import java.util.Map;
import java.util.Properties;

public class TimelineApiApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimelineApiApplication.class);

    private static HttpServer server;
    private static CuratorClient curatorClient;
    private static TimelineReader timelineReader;
    private static CassandraConnector cassandraConnector;
    private static AuthManager authManager;
    private static MetricsCollector metricsCollector;

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        try {
            Map<String, String> parameters = ArgsParser.parse(args);

            Properties properties = PropertiesLoader.load(parameters.getOrDefault("application.properties", "file://application.properties"));

            Properties httpServerProperties = PropertiesUtil.ofScope(properties, Scopes.HTTP_SERVER);
            Properties curatorProperties = PropertiesUtil.ofScope(properties, Scopes.CURATOR);
            Properties cassandraProperties = PropertiesUtil.ofScope(properties, Scopes.CASSANDRA);
            Properties contextProperties = PropertiesUtil.ofScope(properties, Scopes.CONTEXT);
            Properties metricsProperties = PropertiesUtil.ofScope(properties, Scopes.METRICS);

            ApplicationContextHolder.init("Hercules timeline API", "timeline-api", contextProperties);

            curatorClient = new CuratorClient(curatorProperties);
            curatorClient.start();

            cassandraConnector = new CassandraConnector(cassandraProperties);
            cassandraConnector.connect();

            timelineReader = new TimelineReader(cassandraConnector);

            authManager = new AuthManager(curatorClient);
            authManager.start();

            metricsCollector = new MetricsCollector(metricsProperties);
            metricsCollector.start();
            CommonMetrics.registerCommonMetrics(metricsCollector);

            server = new HttpServer(
                    httpServerProperties,
                    authManager,
                    new ReadTimelineHandler(new TimelineRepository(curatorClient), timelineReader, authManager),
                    metricsCollector
            );
            server.start();
        } catch (Throwable t) {
            LOGGER.error("Error on starting timeline api", t);
            shutdown();
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(TimelineApiApplication::shutdown));

        LOGGER.info("Timeline API started for {} millis", System.currentTimeMillis() - start);
    }

    private static void shutdown() {
        long start = System.currentTimeMillis();
        LOGGER.info("Started Timeline API shutdown");
        try {
            if (server != null) {
                server.stop();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping server", t);
            //TODO: Process error
        }

        try {
            if (metricsCollector != null) {
                metricsCollector.stop();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on metrics collector shutdown", t);
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
            if (authManager != null) {
                authManager.stop();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping auth manager", t);
        }

        try {
            if (cassandraConnector != null) {
                cassandraConnector.close();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping cassandra connector", t);
            //TODO: Process error
        }
        try {
            if (timelineReader != null) {
                timelineReader.shutdown();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping time line reader", t);
            //TODO: Process error
        }
        LOGGER.info("Finished Timeline API shutdown for {} millis", System.currentTimeMillis() - start);
    }
}
