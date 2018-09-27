package ru.kontur.vostok.hercules.kafka.util.processing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.configuration.util.ArgsParser;
import ru.kontur.vostok.hercules.configuration.util.PropertiesReader;
import ru.kontur.vostok.hercules.configuration.util.PropertiesUtil;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.util.PatternMatcher;
import ru.kontur.vostok.hercules.util.application.ApplicationContextHolder;
import ru.kontur.vostok.hercules.util.properties.PropertiesExtractor;
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
    protected MetricsCollector metricsCollector;
    private BulkSender sender;

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

        ApplicationContextHolder.init("sink." + getDaemonName(), contextProperties);

        String pattern = PropertiesExtractor.getRequiredProperty(streamProperties, "stream.pattern", String.class);

        metricsCollector = new MetricsCollector(metricsProperties);
        metricsCollector.start();

        //TODO: Validate sinkProperties
        try {
            sender = createSender(sinkProperties);
            bulkEventSink = new CommonBulkEventSink(getDaemonName(), new PatternMatcher(pattern), streamProperties, sender, metricsCollector);

            Thread worker = new Thread(bulkEventSink::run, "sink-worker");
            worker.setUncaughtExceptionHandler((t, e) -> {
                LOGGER.error("Error in worker thread", e);
                this.shutdown();
            });
            worker.start();
        } catch (Throwable e) {
            LOGGER.error("Cannot start " + getDaemonName() + " due to", e);
            shutdown();
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

        LOGGER.info(String.format("%s sink daemon started for %d millis", getDaemonName(), timer.elapsed()));
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
     * @return daemon name for logging, building consumer group name etc.
     */
    protected abstract String getDaemonName();

    private void shutdown() {
        SimpleTimer timer = new SimpleTimer();
        LOGGER.info(String.format("Prepare %s sink daemon to be shutdown", getDaemonName()));

        try {
            if (Objects.nonNull(metricsCollector)) {
                metricsCollector.stop();
            }
        } catch (Throwable t) {
            LOGGER.error("Error on stopping metrics collector", t);
        }

        try {
            if (Objects.nonNull(bulkEventSink)) {
                bulkEventSink.stop(5_000, TimeUnit.MILLISECONDS);
            }
        } catch (Throwable e) {
            LOGGER.error("Error on stopping bulk event sink", e);
            //TODO: Process error
        }

        try {
            if (Objects.nonNull(sender)) {
                sender.close();
            }
        } catch (Throwable e) {
            LOGGER.error("Error on stopping " + getDaemonName(), e);
            //TODO: Process error
        }

        LOGGER.info(String.format("Finished %s sink daemon shutdown for %d millis", getDaemonName(), timer.elapsed()));
    }
}
