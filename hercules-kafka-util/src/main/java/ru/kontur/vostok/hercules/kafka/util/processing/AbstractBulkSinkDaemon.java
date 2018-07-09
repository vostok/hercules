package ru.kontur.vostok.hercules.kafka.util.processing;

import ru.kontur.vostok.hercules.meta.curator.CuratorClient;
import ru.kontur.vostok.hercules.meta.stream.Stream;
import ru.kontur.vostok.hercules.meta.stream.StreamRepository;
import ru.kontur.vostok.hercules.util.args.ArgsParser;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static ru.kontur.vostok.hercules.util.throwable.ThrowableUtil.toUnchecked;

public abstract class AbstractBulkSinkDaemon {

    private CuratorClient curatorClient;
    private CommonBulkEventSink bulkEventSink;
    private BulkEventSender sender;

    public void run(String[] args) {
        long start = System.currentTimeMillis();

        Map<String, String> parameters = ArgsParser.parse(args);

        Properties curatorProperties = PropertiesUtil.readProperties(parameters.getOrDefault("curator.properties", "curator.properties"));
        Properties streamProperties = PropertiesUtil.readProperties(parameters.getOrDefault("streams.properties", "streams.properties"));

        curatorClient = new CuratorClient(curatorProperties);
        curatorClient.start();

        StreamRepository streamRepository = new StreamRepository(curatorClient);

        String streamName = streamProperties.getProperty("stream.name");
        Optional<Stream> stream = toUnchecked(() -> streamRepository.read(streamName));
        if (!stream.isPresent()) {
            throw new IllegalArgumentException("Unknown stream");
        }

        //TODO: Validate sinkProperties
        try {
            sender = createSender(parameters);
            bulkEventSink = new CommonBulkEventSink(getDaemonName(), stream.get(), streamProperties, sender::send);
            bulkEventSink.start();
        } catch (Throwable e) {
            e.printStackTrace();
            shutdown();
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

        System.out.println(String.format("%s sink daemon started for %d millis", getDaemonName(), System.currentTimeMillis() - start));
    }

    protected abstract BulkEventSender createSender(Map<String, String> parameters);

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

        try {
            if (Objects.nonNull(curatorClient)) {
                curatorClient.stop();
            }
        } catch (Throwable e) {
            e.printStackTrace(); //TODO: Process error
        }

        System.out.println(String.format("Finished %s sink daemon shutdown for %d millis", getDaemonName(), System.currentTimeMillis() - start));
    }
}
