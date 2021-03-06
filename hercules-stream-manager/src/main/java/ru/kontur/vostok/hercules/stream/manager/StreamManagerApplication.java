package ru.kontur.vostok.hercules.stream.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.application.Application;
import ru.kontur.vostok.hercules.configuration.PropertiesLoader;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.configuration.util.ArgsParser;
import ru.kontur.vostok.hercules.curator.CuratorClient;
import ru.kontur.vostok.hercules.health.CommonMetrics;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.meta.task.stream.StreamTaskRepository;
import ru.kontur.vostok.hercules.stream.manager.kafka.KafkaManager;
import ru.kontur.vostok.hercules.undertow.util.servers.DaemonHttpServer;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;
import ru.kontur.vostok.hercules.util.validation.IntegerValidators;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @author Gregory Koshelev
 */
public class StreamManagerApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(StreamManagerApplication.class);

    private static KafkaManager kafkaManager;
    private static CuratorClient curatorClient;
    private static StreamTaskExecutor streamTaskExecutor;
    private static DaemonHttpServer daemonHttpServer;
    private static MetricsCollector metricsCollector;

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        try {
            Application.run("Hercules Stream Manager", "stream-manager", args);
            Map<String, String> parameters = ArgsParser.parse(args);

            Properties properties = PropertiesLoader.load(parameters.getOrDefault("application.properties", "file://application.properties"));
            Properties kafkaProperties = PropertiesUtil.ofScope(properties, Scopes.KAFKA);
            Properties curatorProperties = PropertiesUtil.ofScope(properties, Scopes.CURATOR);
            Properties statusServerProperties = PropertiesUtil.ofScope(properties, Scopes.HTTP_SERVER);
            Properties metricsProperties = PropertiesUtil.ofScope(properties, Scopes.METRICS);

            final int replicationFactor = PropertiesUtil.get(Props.REPLICATION_FACTOR, properties).get();

            metricsCollector = new MetricsCollector(metricsProperties);
            metricsCollector.start();
            CommonMetrics.registerCommonMetrics(metricsCollector);

            kafkaManager = new KafkaManager(kafkaProperties, (short) replicationFactor);

            curatorClient = new CuratorClient(curatorProperties);
            curatorClient.start();

            StreamTaskRepository streamTaskRepository = new StreamTaskRepository(curatorClient);
            StreamRepository streamRepository = new StreamRepository(curatorClient);

            streamTaskExecutor =
                    new StreamTaskExecutor(
                            streamTaskRepository,
                            5_000,
                            kafkaManager,
                            streamRepository,
                            metricsCollector);
            streamTaskExecutor.start();

            daemonHttpServer = new DaemonHttpServer(statusServerProperties, metricsCollector);
            daemonHttpServer.start();
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
            if (daemonHttpServer != null) {
                daemonHttpServer.stop(5_000, TimeUnit.MILLISECONDS);
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping http server", t);
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
            LOGGER.error("Error on stopping kafka manager", t);
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

    private static class Props {
        static final Parameter<Integer> REPLICATION_FACTOR =
                Parameter.integerParameter("replicationFactor").
                        withDefault(1).
                        withValidator(IntegerValidators.positive()).
                        build();
    }
}
