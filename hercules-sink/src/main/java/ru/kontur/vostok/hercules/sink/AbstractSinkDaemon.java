package ru.kontur.vostok.hercules.sink;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.application.Application;
import ru.kontur.vostok.hercules.configuration.PropertiesLoader;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.configuration.util.ArgsParser;
import ru.kontur.vostok.hercules.health.CommonMetrics;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.sink.metrics.SinkMetrics;
import ru.kontur.vostok.hercules.undertow.util.servers.DaemonHttpServer;
import ru.kontur.vostok.hercules.util.concurrent.ThreadFactories;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Base implementation of Sink daemon. Uses pool of SenderSink for concurrent processing.
 *
 * @author Gregory Koshelev
 */
public abstract class AbstractSinkDaemon {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSinkDaemon.class);
    private static final long SHUTDOWN_TIMEOUT_MS = 5_000L;

    protected MetricsCollector metricsCollector;

    private Sender sender;
    private ExecutorService executor;
    private SinkPool sinkPool;

    private DaemonHttpServer daemonHttpServer;

    protected void run(String[] args) {
        long start = System.currentTimeMillis();
        LOGGER.info("Starting {}", getDaemonName());

        try {
            Map<String, String> parameters = ArgsParser.parse(args);

            Properties properties = PropertiesLoader.load(parameters.getOrDefault("application.properties", "file://application.properties"));

            Properties metricsProperties = PropertiesUtil.ofScope(properties, Scopes.METRICS);
            Properties httpServerProperties = PropertiesUtil.ofScope(properties, Scopes.HTTP_SERVER);
            Properties sinkProperties = PropertiesUtil.ofScope(properties, Scopes.SINK);

            Properties senderProperties = PropertiesUtil.ofScope(sinkProperties, Scopes.SENDER);

            String daemonId = getDaemonId();

            Application.run(getDaemonName(), getDaemonId(), args);

            metricsCollector = new MetricsCollector(metricsProperties);
            metricsCollector.start();
            CommonMetrics.registerCommonMetrics(metricsCollector);
            SinkMetrics sinkMetrics = new SinkMetrics(metricsCollector);

            sender = createSender(senderProperties, metricsCollector);
            sender.start();

            int poolSize = PropertiesUtil.get(Props.POOL_SIZE, sinkProperties).get();
            executor = Executors.newFixedThreadPool(poolSize, ThreadFactories.newNamedThreadFactory("sink", false));

            sinkPool =
                    new SinkPool(
                            poolSize,
                            () -> new SenderSink(
                                    executor,
                                    daemonId,
                                    sinkProperties,
                                    sender,
                                    sinkMetrics));
            sinkPool.start();

            daemonHttpServer = new DaemonHttpServer(httpServerProperties, metricsCollector);
            daemonHttpServer.start();
        } catch (Throwable throwable) {
            LOGGER.error("Cannot start application due to error", throwable);
            shutdown();
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

        LOGGER.info("{} started for {} millis", getDaemonName(), System.currentTimeMillis() - start);
    }

    protected abstract Sender createSender(Properties senderProperties, MetricsCollector metricsCollector);

    /**
     * Application id is used to identify across Hercules Cluster. E.g. in metrics, logging and others.
     *
     * @return application id
     */
    protected abstract String getDaemonId();

    /**
     * Human readable application name.
     *
     * @return application name
     */
    protected abstract String getDaemonName();

    private void shutdown() {
        long start = System.currentTimeMillis();

        LOGGER.info("Start {} shutdown", getDaemonName());

        try {
            if (daemonHttpServer != null) {
                daemonHttpServer.stop(SHUTDOWN_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping HTTP Server", t);
        }

        try {
            if (sinkPool != null) {
                sinkPool.stop(SHUTDOWN_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping Sink pool", t);
        }

        try {
            if (executor != null) {
                executor.shutdown();
                if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                    executor.shutdownNow();
                    if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                        LOGGER.warn("Thread pool did not terminate");
                    }
                }
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping sink thread executor", t);
        }

        try {
            if (sender != null) {
                sender.stop(SHUTDOWN_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping sender", t);
        }

        try {
            if (metricsCollector != null) {
                metricsCollector.stop();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping metrics collector", t);
        }

        LOGGER.info("Finished {} shutdown for {} millis", getDaemonName(), System.currentTimeMillis() - start);
    }

    private static class Props {
        static final Parameter<Integer> POOL_SIZE =
                Parameter.integerParameter("poolSize").
                        withDefault(1).
                        build();
    }
}
