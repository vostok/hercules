package ru.kontur.vostok.hercules.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.configuration.util.ArgsParser;
import ru.kontur.vostok.hercules.configuration.util.PropertiesReader;
import ru.kontur.vostok.hercules.configuration.util.PropertiesUtil;
import ru.kontur.vostok.hercules.meta.curator.CuratorClient;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.metrics.MetricsCollector;
import ru.kontur.vostok.hercules.partitioner.HashPartitioner;
import ru.kontur.vostok.hercules.partitioner.NaiveHasher;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class GatewayApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayApplication.class);

    private static MetricsCollector metricsCollector;
    private static HttpServer server;
    private static EventSender eventSender;
    private static CuratorClient curatorClient;
    private static AuthManager authManager;

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        try {
            Map<String, String> parameters = ArgsParser.parse(args);

            Properties properties = PropertiesReader.read(parameters.getOrDefault("application.properties", "application.properties"));

            Properties httpserverProperties = PropertiesUtil.ofScope(properties, Scopes.HTTP_SERVER);
            Properties producerProperties = PropertiesUtil.ofScope(properties, Scopes.PRODUCER);
            Properties curatorProperties = PropertiesUtil.ofScope(properties, Scopes.CURATOR);
            Properties metricsProperties = PropertiesUtil.ofScope(properties, Scopes.METRICS);

            metricsCollector = new MetricsCollector(metricsProperties);
            metricsCollector.start();

            eventSender = new EventSender(producerProperties, new HashPartitioner(new NaiveHasher()));

            curatorClient = new CuratorClient(curatorProperties);
            curatorClient.start();

            StreamRepository streamRepository = new StreamRepository(curatorClient);

            authManager = new AuthManager(curatorClient);
            authManager.start();

            server = new HttpServer(metricsCollector, httpserverProperties, authManager, eventSender, streamRepository);
            server.start();
        } catch (Throwable e) {
            LOGGER.error("Cannot start application due to", e);
            shutdown();
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(GatewayApplication::shutdown));

        LOGGER.info("Gateway started for " + (System.currentTimeMillis() - start) + " millis" );
    }

    private static void shutdown() {
        long start = System.currentTimeMillis();
        LOGGER.info("Started Gateway shutdown");
        try {
            if (server != null) {
                server.stop();
            }
        } catch (Throwable e) {
            LOGGER.error("Error on http server shutdown", e);
            //TODO: Process error
        }

        try {
            if (eventSender != null) {
                eventSender.stop(5_000, TimeUnit.MILLISECONDS);
            }
        } catch (Throwable e) {
            LOGGER.error("Error on event sender shutdown", e);
            //TODO: Process error
        }

        try {
            if (authManager != null) {
                authManager.stop();
            }
        } catch (Throwable e) {
            LOGGER.error("Error on auth manager shutdown", e);
        }

        try {
            if (curatorClient != null) {
                curatorClient.stop();
            }
        } catch (Throwable e) {
            LOGGER.error("Error on curator client shutdown", e);
            //TODO: Process error
        }

        try {
            if (metricsCollector != null) {
                metricsCollector.stop();
            }
        } catch (Throwable e) {
            LOGGER.error("Error on metrics collector shutdown", e);
            //TODO: Process error
        }

        LOGGER.info("Finished Gateway shutdown for " + (System.currentTimeMillis() - start) + " millis");
    }
}
