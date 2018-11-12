package ru.kontur.vostok.hercules.kafka.util.processing.bulk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.configuration.util.ArgsParser;
import ru.kontur.vostok.hercules.configuration.util.PropertiesReader;
import ru.kontur.vostok.hercules.configuration.util.PropertiesUtil;
import ru.kontur.vostok.hercules.health.CommonMetrics;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.undertow.util.servers.ApplicationStatusHttpServer;
import ru.kontur.vostok.hercules.util.application.ApplicationContextHolder;
import ru.kontur.vostok.hercules.util.time.SimpleTimer;

import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Abstract sink daemon implementation
 */
public abstract class AbstractBulkSinkDaemon {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractBulkSinkDaemon.class);

    private CommonBulkEventSink bulkEventSink;
    private ApplicationStatusHttpServer applicationStatusHttpServer;
    protected MetricsCollector metricsCollector;

    /**
     * Start daemon
     *
     * @param args command line arguments
     */
    public void run(String[] args) {
        SimpleTimer timer = new SimpleTimer();

        Map<String, String> parameters = ArgsParser.parse(args);

        Properties properties = PropertiesReader.read(parameters.getOrDefault("application.properties", "application.properties"));
        Properties streamProperties = PropertiesUtil.ofScope(properties, Scopes.STREAMS);
        Properties sinkProperties = PropertiesUtil.ofScope(properties, Scopes.SINK);
        Properties metricsProperties = PropertiesUtil.ofScope(properties, Scopes.METRICS);
        Properties contextProperties = PropertiesUtil.ofScope(properties, Scopes.CONTEXT);
        Properties httpServerProperties = PropertiesUtil.ofScope(properties, Scopes.HTTP_SERVER);

        final String daemonId = getDaemonId();
        if (!daemonId.startsWith("sink.")) {
            throw new IllegalStateException(String.format("Daemon id must starts with 'sink.' but was '%s'", daemonId));
        }
        ApplicationContextHolder.init(getDaemonName(), daemonId, contextProperties);

        metricsCollector = new MetricsCollector(metricsProperties);
        metricsCollector.start();
        CommonMetrics.registerMemoryMetrics(metricsCollector);

        applicationStatusHttpServer = new ApplicationStatusHttpServer(httpServerProperties);
        applicationStatusHttpServer.start();

        //TODO: Validate sinkProperties
        try {
            bulkEventSink = new CommonBulkEventSink(
                    daemonId,
                    streamProperties,
                    sinkProperties,
                    () -> createSender(sinkProperties),
                    metricsCollector
            );

            bulkEventSink.start();
        } catch (Throwable e) {
            LOGGER.error("Cannot start " + getDaemonName() + " due to", e);
            shutdown();
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

        LOGGER.info(String.format("%s daemon started for %d millis", getDaemonName(), timer.elapsed()));
    }

    /**
     * Create instance of BulkSender which processes bulk of events
     * Must be implemented in descendants
     *
     * @param sinkProperties properties for sender implementation
     * @return sender instance
     */
    protected abstract BulkSender createSender(Properties sinkProperties);

    /**
     * @return human readable daemon name
     */
    protected abstract String getDaemonName();

    /**
     * @return daemon id for metrics, logging etc.
     */
    protected abstract String getDaemonId();

    private void shutdown() {
        SimpleTimer timer = new SimpleTimer();
        LOGGER.info(String.format("Prepare %s daemon to be shutdown", getDaemonName()));

        try {
            if (Objects.nonNull(metricsCollector)) {
                metricsCollector.stop();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping metrics collector", t);
        }

        try {
            if (Objects.nonNull(applicationStatusHttpServer)) {
                applicationStatusHttpServer.stop();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping minimal status server", t);
        }

        try {
            if (Objects.nonNull(bulkEventSink)) {
                bulkEventSink.stop(5_000, TimeUnit.MILLISECONDS);
            }
        } catch (Throwable e) {
            LOGGER.error("Error on stopping bulk event sink", e);
            //TODO: Process error
        }

        LOGGER.info(String.format("Finished %s daemon shutdown for %d millis", getDaemonName(), timer.elapsed()));
    }
}
