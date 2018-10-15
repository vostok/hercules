package ru.kontur.vostok.hercules.gate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.configuration.util.ArgsParser;
import ru.kontur.vostok.hercules.configuration.util.PropertiesReader;
import ru.kontur.vostok.hercules.configuration.util.PropertiesUtil;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.meta.curator.CuratorClient;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.meta.stream.StreamStorage;
import ru.kontur.vostok.hercules.partitioner.HashPartitioner;
import ru.kontur.vostok.hercules.partitioner.NaiveHasher;
import ru.kontur.vostok.hercules.util.application.ApplicationContextHolder;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class GateApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(GateApplication.class);

    private static MetricsCollector metricsCollector;
    private static HttpServer server;
    private static EventSender eventSender;
    private static CuratorClient curatorClient;
    private static AuthManager authManager;
    private static AuthValidationManager authValidationManager;

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        try {
            Map<String, String> parameters = ArgsParser.parse(args);

            Properties properties = PropertiesReader.read(parameters.getOrDefault("application.properties", "application.properties"));

            Properties httpserverProperties = PropertiesUtil.ofScope(properties, Scopes.HTTP_SERVER);
            Properties producerProperties = PropertiesUtil.ofScope(properties, Scopes.PRODUCER);
            Properties curatorProperties = PropertiesUtil.ofScope(properties, Scopes.CURATOR);
            Properties metricsProperties = PropertiesUtil.ofScope(properties, Scopes.METRICS);
            Properties contextProperties = PropertiesUtil.ofScope(properties, Scopes.CONTEXT);

            ApplicationContextHolder.init("gate", contextProperties);

            metricsCollector = new MetricsCollector(metricsProperties);
            metricsCollector.start();

            eventSender = new EventSender(producerProperties, new HashPartitioner(new NaiveHasher()));

            curatorClient = new CuratorClient(curatorProperties);
            curatorClient.start();

            StreamRepository streamRepository = new StreamRepository(curatorClient);
            StreamStorage streamStorage = new StreamStorage(streamRepository);

            authManager = new AuthManager(curatorClient);
            authManager.start();

            authValidationManager = new AuthValidationManager(curatorClient);
            authValidationManager.start();

            server = new HttpServer(metricsCollector, httpserverProperties, authManager, authValidationManager, eventSender, streamStorage);
            server.start();
        } catch (Throwable t) {
            LOGGER.error("Cannot start application due to", t);
            shutdown();
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(GateApplication::shutdown));

        LOGGER.info("Gateway started for {} millis", System.currentTimeMillis() - start);
    }

    private static void shutdown() {
        long start = System.currentTimeMillis();
        LOGGER.info("Started Gateway shutdown");
        try {
            if (server != null) {
                server.stop();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on http server shutdown", t);
            //TODO: Process error
        }

        try {
            if (eventSender != null) {
                eventSender.stop(5_000, TimeUnit.MILLISECONDS);
            }
        } catch (Throwable t) {
            LOGGER.error("Error on event sender shutdown", t);
            //TODO: Process error
        }

        try {
            if (authManager != null) {
                authManager.stop();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on auth manager shutdown", t);
        }

        try {
            if (authValidationManager != null) {
                authValidationManager.stop();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping auth validation manager", t);
        }

        try {
            if (curatorClient != null) {
                curatorClient.stop();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on curator client shutdown", t);
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

        LOGGER.info("Finished Gateway shutdown for {}  millis", System.currentTimeMillis() - start);
    }
}
