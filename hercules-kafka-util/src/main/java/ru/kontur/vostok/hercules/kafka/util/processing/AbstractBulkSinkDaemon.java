package ru.kontur.vostok.hercules.kafka.util.processing;

import ru.kontur.vostok.hercules.configuration.Scopes;
import ru.kontur.vostok.hercules.configuration.util.ArgsParser;
import ru.kontur.vostok.hercules.configuration.util.PropertiesReader;
import ru.kontur.vostok.hercules.configuration.util.PropertiesUtil;
import ru.kontur.vostok.hercules.util.PatternMatcher;
import ru.kontur.vostok.hercules.util.properties.PropertiesExtractor;

import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Abstract sink daemon implementation
 */
public abstract class AbstractBulkSinkDaemon {

    private CommonBulkEventSink bulkEventSink;
    private BulkSender sender;

    /**
     * Start daemon
     *
     * @param args command line arguments
     */
    public void run(String[] args) {
        long start = System.currentTimeMillis();

        Map<String, String> parameters = ArgsParser.parse(args);

        Properties properties = PropertiesReader.read(parameters.getOrDefault("application.properties", "application.properties"));
        Properties streamProperties = PropertiesUtil.ofScope(properties, Scopes.STREAMS);
        Properties sinkProperties = PropertiesUtil.ofScope(properties, Scopes.SINK);

        String pattern = PropertiesExtractor.getRequiredProperty(streamProperties, "stream.name", String.class);

        //TODO: Validate sinkProperties
        try {
            sender = createSender(sinkProperties);
            bulkEventSink = new CommonBulkEventSink(getDaemonName(), new PatternMatcher(pattern), streamProperties, sender);
            bulkEventSink.start();
        } catch (Throwable e) {
            e.printStackTrace();
            shutdown();
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

        System.out.println(String.format("%s sink daemon started for %d millis", getDaemonName(), System.currentTimeMillis() - start));
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
        long start = System.currentTimeMillis();
        System.out.println(String.format("Prepare %s sink daemon to be shutdown", getDaemonName()));

        try {
            if (Objects.nonNull(bulkEventSink)) {
                bulkEventSink.stop(5_000, TimeUnit.MILLISECONDS);
            }
        } catch (Throwable e) {
            e.printStackTrace(); //TODO: Process error
        }

        try {
            if (Objects.nonNull(sender)) {
                sender.close();
            }
        } catch (Throwable e) {
            e.printStackTrace(); //TODO: Process error
        }

        System.out.println(String.format("Finished %s sink daemon shutdown for %d millis", getDaemonName(), System.currentTimeMillis() - start));
    }
}
