package ru.kontur.hercules.tracing.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.cassandra.util.CassandraConnector;
import ru.kontur.vostok.hercules.configuration.PropertiesLoader;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.configuration.util.ArgsParser;
import ru.kontur.vostok.hercules.configuration.util.PropertiesUtil;
import ru.kontur.vostok.hercules.curator.CuratorClient;
import ru.kontur.vostok.hercules.health.CommonMetrics;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.util.application.ApplicationContextHolder;

import java.util.Map;
import java.util.Properties;

/**
 * TracingApiApplication
 *
 * @author Kirill Sulim
 */
public class TracingApiApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(TracingApiApplication.class);

    private static HttpServer server;
    private static CuratorClient curatorClient;
    private static CassandraConnector cassandraConnector;
    private static MetricsCollector metricsCollector;
    private static CassandraTracingReader cassandraTracingReader;

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

            ApplicationContextHolder.init("Hercules tracing API", "tracing-api", contextProperties);

            curatorClient = new CuratorClient(curatorProperties);
            curatorClient.start();

            cassandraConnector = new CassandraConnector(cassandraProperties);
            cassandraConnector.connect();

            cassandraTracingReader = new CassandraTracingReader(cassandraConnector);

            metricsCollector = new MetricsCollector(metricsProperties);
            metricsCollector.start();
            CommonMetrics.registerCommonMetrics(metricsCollector);

            server = new HttpServer(
                httpServerProperties,
                new GetTraceHandler(cassandraTracingReader),
                metricsCollector
            );
            server.start();
        } catch (Throwable t) {
            LOGGER.error("Error on starting Tracing API", t);
            shutdown();
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(TracingApiApplication::shutdown));

        LOGGER.info("Tracing API started for {} millis", System.currentTimeMillis() - start);
    }

    private static void shutdown() {
        long start = System.currentTimeMillis();
        LOGGER.info("Started Tracing API shutdown");
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
            if (cassandraConnector != null) {
                cassandraConnector.close();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping cassandra connector", t);
            //TODO: Process error
        }

        LOGGER.info("Finished Tracing API shutdown for {} millis", System.currentTimeMillis() - start);
    }
}
