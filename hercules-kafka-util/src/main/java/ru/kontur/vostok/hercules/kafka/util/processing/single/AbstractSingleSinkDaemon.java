package ru.kontur.vostok.hercules.kafka.util.processing.single;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.configuration.util.ArgsParser;
import ru.kontur.vostok.hercules.configuration.util.PropertiesReader;
import ru.kontur.vostok.hercules.configuration.util.PropertiesUtil;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.undertow.util.servers.ApplicationStatusHttpServer;
import ru.kontur.vostok.hercules.util.application.ApplicationContextHolder;
import ru.kontur.vostok.hercules.util.time.SimpleTimer;

import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;

/**
 * AbstractSingleSinkDaemon
 *
 * @author Kirill Sulim
 */
public abstract class AbstractSingleSinkDaemon {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSingleSinkDaemon.class);

    private CommonSingleSink singleSink;
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

        try {
            metricsCollector = new MetricsCollector(metricsProperties);
            metricsCollector.start();

            applicationStatusHttpServer = new ApplicationStatusHttpServer(httpServerProperties);
            applicationStatusHttpServer.start();

            initSink(properties);

            //TODO: Validate sinkProperties
            singleSink = new CommonSingleSink(
                    daemonId,
                    streamProperties,
                    sinkProperties,
                    () -> createSender(sinkProperties),
                    metricsCollector
            );
            singleSink.start();
        } catch (Throwable e) {
            LOGGER.error("Cannot start " + getDaemonName() + " due to", e);
            shutdown();
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

        LOGGER.info(String.format("%s daemon started for %d millis", getDaemonName(), timer.elapsed()));
    }

    /**
     * Create instance of SingleSender which processes bulk of events
     * Must be implemented in descendants
     *
     * @param sinkProperties sink properties
     * @return sender instance
     */
    protected abstract SingleSender<UUID, Event> createSender(Properties sinkProperties);

    /**
     * @return human readable daemon name
     */
    protected abstract String getDaemonName();

    /**
     * @return daemon id for metrics, logging etc.
     */
    protected abstract String getDaemonId();

    /**
     * Perform sink-dependant shutdown
     */
    protected void shutdownSink() {
        /* should be overwritten in descendant */
    }

    /**
     * Perform sink-depoendent init
     *
     * @param properties copy of applicationProperties
     */
    protected void initSink(Properties properties) {
        /* should be overwritten in descendant */
    }

    private void shutdown() {
        SimpleTimer timer = new SimpleTimer();
        LOGGER.info(String.format("Prepare %s daemon to be shutdown", getDaemonName()));

        try {
            if (Objects.nonNull(singleSink)) {
                singleSink.stop();
            }
        } catch (Throwable e) {
            LOGGER.error("Error on stopping event sink", e);
            //TODO: Process error
        }

        try {
            shutdownSink();
        }
        catch (Throwable t) {
            LOGGER.error("Should never happen, all throwables must be catched and processed inside shutdownSink method", t);
        }

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

        LOGGER.info(String.format("Finished %s daemon shutdown for %d millis", getDaemonName(), timer.elapsed()));
    }
}
