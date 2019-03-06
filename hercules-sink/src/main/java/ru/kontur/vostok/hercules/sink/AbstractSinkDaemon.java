package ru.kontur.vostok.hercules.sink;

import com.codahale.metrics.Meter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.configuration.PropertiesLoader;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.configuration.util.ArgsParser;
import ru.kontur.vostok.hercules.configuration.util.PropertiesUtil;
import ru.kontur.vostok.hercules.health.CommonMetrics;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.undertow.util.servers.ApplicationStatusHttpServer;
import ru.kontur.vostok.hercules.util.application.ApplicationContextHolder;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Base implementation of Sink daemon. Uses pool of SimpleSink for concurrent processing.
 *
 * @author Gregory Koshelev
 */
public abstract class AbstractSinkDaemon {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSinkDaemon.class);

    private Sender sender;
    private SinkPool sinkPool;
    private ExecutorService executor;
    private ApplicationStatusHttpServer applicationStatusHttpServer;

    protected MetricsCollector metricsCollector;

    protected void run(String[] args) {
        long start = System.currentTimeMillis();
        LOGGER.info("Starting {}", getDaemonName());

        try {
            Map<String, String> parameters = ArgsParser.parse(args);

            Properties properties = PropertiesLoader.load(parameters.getOrDefault("application.properties", "file://applicationProperties"));

            Properties contextProperties = PropertiesUtil.ofScope(properties, Scopes.CONTEXT);
            Properties metricsProperties = PropertiesUtil.ofScope(properties, Scopes.METRICS);
            Properties httpServerProperties = PropertiesUtil.ofScope(properties, Scopes.HTTP_SERVER);
            Properties sinkProperties = PropertiesUtil.ofScope(properties, Scopes.SINK);

            Properties senderProperties = PropertiesUtil.ofScope(sinkProperties, Scopes.SENDER);

            String daemonId = getDaemonId();
            ApplicationContextHolder.init(getDaemonName(), getDaemonId(), contextProperties);

            metricsCollector = new MetricsCollector(metricsProperties);
            metricsCollector.start();
            CommonMetrics.registerCommonMetrics(
                    metricsCollector,
                    Props.THREAD_GROUP_REGEXP.extract(metricsProperties));

            applicationStatusHttpServer = new ApplicationStatusHttpServer(httpServerProperties);
            applicationStatusHttpServer.start();

            this.sender = createSender(senderProperties, metricsCollector);
            sender.start();

            int poolSize = Props.POOL_SIZE.extract(sinkProperties);
            this.executor = Executors.newFixedThreadPool(poolSize);//TODO: Provide custom ThreadFactory

            Meter droppedEventsMeter = metricsCollector.meter("droppedEvents");
            Meter processedEventsMeter = metricsCollector.meter("processedEvents");
            Meter rejectedEventsMeter = metricsCollector.meter("rejectedEvents");
            Meter totalEventsMeter = metricsCollector.meter("totalEvents");

            this.sinkPool =
                    new SinkPool(
                            poolSize,
                            () -> new SimpleSink(
                                    executor,
                                    daemonId,
                                    sinkProperties,
                                    sender,
                                    droppedEventsMeter,
                                    processedEventsMeter,
                                    rejectedEventsMeter,
                                    totalEventsMeter));
            sinkPool.start();
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
            if (sinkPool != null) {
                sinkPool.stop();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping Sink pool", t);
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
                sender.stop(5_000L, TimeUnit.MILLISECONDS);
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping sender", t);
        }

        try {
            if (applicationStatusHttpServer != null) {
                applicationStatusHttpServer.stop();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping http status server", t);
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
        static final PropertyDescription<Integer> POOL_SIZE =
                PropertyDescriptions.integerProperty("poolSize").withDefaultValue(1).build();

        static final PropertyDescription<String[]> THREAD_GROUP_REGEXP =
                PropertyDescriptions.arrayOfStringsProperty("thread.group.regexp").
                        withDefaultValue(new String[]{}).
                        build();
    }
}
