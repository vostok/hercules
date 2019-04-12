package ru.kontur.vostok.hercules.stream.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.configuration.PropertiesLoader;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.configuration.util.ArgsParser;
import ru.kontur.vostok.hercules.configuration.util.PropertiesUtil;
import ru.kontur.vostok.hercules.curator.CuratorClient;
import ru.kontur.vostok.hercules.health.CommonMetrics;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.meta.task.stream.StreamTaskRepository;
import ru.kontur.vostok.hercules.undertow.util.servers.ApplicationStatusHttpServer;
import ru.kontur.vostok.hercules.util.application.ApplicationContextHolder;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;
import ru.kontur.vostok.hercules.util.validation.Validators;

import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class StreamManagerApplication {

    private static class Props {
        static final PropertyDescription<Short> REPLICATION_FACTOR = PropertyDescriptions
                .shortProperty("replicationFactor")
                .withDefaultValue((short) 1)
                .withValidator(Validators.greaterThan((short) 0))
                .build();
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(StreamManagerApplication.class);

    private static KafkaManager kafkaManager;
    private static CuratorClient curatorClient;
    private static StreamTaskExecutor streamTaskExecutor;
    private static ApplicationStatusHttpServer applicationStatusHttpServer;
    private static MetricsCollector metricsCollector;

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        try {
            Map<String, String> parameters = ArgsParser.parse(args);

            Properties properties = PropertiesLoader.load(parameters.getOrDefault("application.properties", "file://application.properties"));
            Properties kafkaProperties = PropertiesUtil.ofScope(properties, Scopes.KAFKA);
            Properties curatorProperties = PropertiesUtil.ofScope(properties, Scopes.CURATOR);
            Properties statusServerProperties = PropertiesUtil.ofScope(properties, Scopes.HTTP_SERVER);
            Properties contextProperties = PropertiesUtil.ofScope(properties, Scopes.CONTEXT);
            Properties metricsProperties = PropertiesUtil.ofScope(properties, Scopes.METRICS);

            final short replicationFactor = Props.REPLICATION_FACTOR.extract(properties);

            ApplicationContextHolder.init("Hercules Stream manager", "stream-manager", contextProperties);

            kafkaManager = new KafkaManager(kafkaProperties, replicationFactor);

            curatorClient = new CuratorClient(curatorProperties);
            curatorClient.start();

            StreamTaskRepository streamTaskRepository = new StreamTaskRepository(curatorClient);
            StreamRepository streamRepository = new StreamRepository(curatorClient);

            metricsCollector = new MetricsCollector(metricsProperties);
            metricsCollector.start();
            CommonMetrics.registerCommonMetrics(metricsCollector);

            streamTaskExecutor =
                    new StreamTaskExecutor(
                            streamTaskRepository,
                            5_000,
                            kafkaManager,
                            streamRepository,
                            metricsCollector);
            streamTaskExecutor.start();

            applicationStatusHttpServer = new ApplicationStatusHttpServer(statusServerProperties);
            applicationStatusHttpServer.start();
        } catch (Throwable t) {
            LOGGER.error("Error on starting kafka manager", t);
            shutdown();
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(StreamManagerApplication::shutdown));

        LOGGER.info("Stream Manager started for {} millis", System.currentTimeMillis() - start);
    }

    private static void shutdown() {
        long start = System.currentTimeMillis();
        LOGGER.info("Started Stream Manager shutdown");
        try {
            if (applicationStatusHttpServer != null) {
                applicationStatusHttpServer.stop();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping kafka manager", t);
            //TODO: Process error
        }

        try {
            if (streamTaskExecutor != null) {
                streamTaskExecutor.stop(5_000, TimeUnit.MILLISECONDS);
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping task executor", t);
            //TODO: Process error
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
            if (kafkaManager != null) {
                kafkaManager.close(5_000, TimeUnit.MILLISECONDS);
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping stream manager", t);
            //TODO: Process error
        }

        try {
            if (metricsCollector != null) {
                metricsCollector.stop();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping metrics collector", t);
        }

        LOGGER.info("Finished Stream Manager shutdown for {} millis", System.currentTimeMillis() - start);
    }
}
