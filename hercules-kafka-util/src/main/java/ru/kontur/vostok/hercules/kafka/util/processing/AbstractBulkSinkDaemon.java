package ru.kontur.vostok.hercules.kafka.util.processing;

import ru.kontur.vostok.hercules.util.PatternMatcher;
import ru.kontur.vostok.hercules.util.args.ArgsParser;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public abstract class AbstractBulkSinkDaemon {

    private CommonBulkEventSink bulkEventSink;
    private BulkSender sender;

    public void run(String[] args) {
        long start = System.currentTimeMillis();

        Map<String, String> parameters = ArgsParser.parse(args);

        Properties streamProperties = PropertiesUtil.readProperties(parameters.getOrDefault("streams.properties", "streams.properties"));

        String pattern = PropertiesUtil.getRequiredProperty(streamProperties, "stream.name", String.class);

        //TODO: Validate sinkProperties
        try {
            sender = createSender(parameters);
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

    protected abstract BulkSender createSender(Map<String, String> parameters);

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
